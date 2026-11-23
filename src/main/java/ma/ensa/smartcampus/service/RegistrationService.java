package ma.ensa.smartcampus.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import ma.ensa.smartcampus.dto.*;
import ma.ensa.smartcampus.entity.*;
import ma.ensa.smartcampus.exception.*;
import ma.ensa.smartcampus.repository.InscriptionRepository;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private static final Duration LINK_LIFETIME = Duration.ofMinutes(30);
    private static final Duration SETUP_LIFETIME = Duration.ofMinutes(10);
    private static final Duration LOCKOUT = Duration.ofMinutes(15);
    private final AccountCreationService accounts;
    private final UserRepository users;
    private final InscriptionRepository inscriptions;
    private final EnrollmentSecrets secrets;
    private final PasswordEncoder passwords;
    private final Clock clock;

    @Transactional
    public void register(RegistrationRequest request) {
        String email = institutionalEmail(request.email());
        validatePasswordLength(request.password());
        Utilisateur user = switch (request.role()) {
            case STUDENT -> {
                var p = request.studentProfile();
                if (p == null || request.teacherProfile() != null) {
                    throw new InvalidRegistrationException("Le profil étudiant exige le CNE et la filière.");
                }
                yield accounts.student(email, request.password(), request.firstName(), request.lastName(), p.cne(), p.filiere());
            }
            case TEACHER -> {
                var p = request.teacherProfile();
                if (p == null || request.studentProfile() != null) {
                    throw new InvalidRegistrationException("Le profil enseignant exige le matricule et le département.");
                }
                yield accounts.teacher(email, request.password(), request.firstName(), request.lastName(), p.matricule(), p.departement());
            }
        };
        if (users.existsByEmail(email)) {
            return;
        }
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                throw new DuplicateRegistrationException();
            }
            throw ex;
        }
        var entry = new Inscription();
        entry.setUser(user);
        issueEmailLink(entry, clock.instant());
        inscriptions.save(entry);
    }

    @Transactional
    public void resend(LoginRequest request) {
        String email = institutionalEmail(request.email());
        validatePasswordLength(request.password());
        inscriptions.lockByEmail(email)
                .ifPresent(entry -> {
                    Instant now = clock.instant();
                    if (entry.getUser().getStatus() != AccountStatus.EN_ATTENTE
                            || entry.getUser().getMfaActivatedAt() != null || blocked(entry, now)
                            || now.isBefore(entry.getLastLinkIssuedAt().plusSeconds(60))) {
                        return;
                    }
                    if (passwords.matches(request.password(), entry.getUser().getPasswordHash())) {
                        issueEmailLink(entry, now);
                    } else {
                        failedProof(entry, now);
                    }
                });
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public MfaSetup confirmEmail(EmailConfirmation request) {
        var entry = inscriptions.findByEmailTokenHash(secrets.hash(request.token())).orElseThrow(this::invalidProof);
        Instant now = clock.instant();
        requirePending(entry, entry.getEmailExpiresAt(), now);
        validatePasswordLength(request.password());
        if (!passwords.matches(request.password(), entry.getUser().getPasswordHash())) {
            failedProof(entry, now);
            throw invalidProof();
        }

        entry.setEmailTokenHash(null);
        entry.setEmailExpiresAt(null);
        entry.setEncryptedMailToken(null);
        entry.setNextMailAttemptAt(null);
        if (entry.getUser().getEmailVerifiedAt() == null) {
            entry.getUser().setEmailVerifiedAt(now);
        }
        entry.setFailedProofs(0);
        entry.setBlockedUntil(null);
        String setupToken = secrets.token();
        String manualKey = secrets.newTotpKey();
        entry.setSetupTokenHash(secrets.hash(setupToken));
        entry.setSetupExpiresAt(now.plus(SETUP_LIFETIME));
        entry.setEncryptedTotpSecret(secrets.encrypt(manualKey, "totp:" + entry.getUserId()));
        String label = UriUtils.encodePathSegment(entry.getUser().getEmail(), StandardCharsets.UTF_8);
        String uri = "otpauth://totp/SmartCampus:" + label + "?secret=" + manualKey
                + "&issuer=SmartCampus&algorithm=SHA1&digits=6&period=30";
        return new MfaSetup(setupToken, manualKey, uri, entry.getSetupExpiresAt());
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public void activate(MfaActivation request) {
        var entry = inscriptions.findBySetupTokenHash(secrets.hash(request.setupToken())).orElseThrow(this::invalidProof);
        Instant now = clock.instant();
        requirePending(entry, entry.getSetupExpiresAt(), now);
        if (entry.getUser().getEmailVerifiedAt() == null) {
            throw invalidProof();
        }
        String key = secrets.decrypt(entry.getEncryptedTotpSecret(), "totp:" + entry.getUserId());
        if (!secrets.matches(key, request.code(), now)) {
            failedProof(entry, now);
            throw invalidProof();
        }
        entry.getUser().setMfaActivatedAt(now);
        entry.setSetupTokenHash(null);
        entry.setSetupExpiresAt(null);
        entry.setEmailTokenHash(null);
        entry.setEmailExpiresAt(null);
        entry.setEncryptedMailToken(null);
        entry.setNextMailAttemptAt(null);
        entry.setFailedProofs(0);
        entry.setBlockedUntil(null);
    }

    private void issueEmailLink(Inscription entry, Instant now) {
        String token = secrets.token();
        entry.setEmailTokenHash(secrets.hash(token));
        entry.setEmailExpiresAt(now.plus(LINK_LIFETIME));
        entry.setEncryptedMailToken(secrets.encrypt(token, "mail:" + entry.getUser().getId()));
        entry.setNextMailAttemptAt(now);
        entry.setLastLinkIssuedAt(now);
    }

    private void requirePending(Inscription entry, Instant expiresAt, Instant now) {
        if (entry.getUser().getStatus() != AccountStatus.EN_ATTENTE || entry.getUser().getMfaActivatedAt() != null
                || expiresAt == null || !now.isBefore(expiresAt)) {
            throw invalidProof();
        }
        if (blocked(entry, now)) {
            throw new LoginLimitExceededException();
        }
    }

    private boolean blocked(Inscription entry, Instant now) {
        return entry.getBlockedUntil() != null && now.isBefore(entry.getBlockedUntil());
    }

    private void failedProof(Inscription entry, Instant now) {
        entry.setFailedProofs(entry.getFailedProofs() + 1);
        if (entry.getFailedProofs() >= 5) {
            entry.setBlockedUntil(now.plus(LOCKOUT));
            entry.setFailedProofs(0);
        }
    }

    private BadCredentialsException invalidProof() {
        return new BadCredentialsException("Vérification invalide.");
    }

    private String institutionalEmail(String raw) {
        String email = AccountService.normalizeEmail(raw);
        try {
            var address = new InternetAddress(email, true);
            address.validate();
            if (email.equals(address.getAddress()) && email.endsWith("@usms.ac.ma")) {
                return email;
            }
        } catch (AddressException ignored) {
            // The same validation message is used for malformed and non-institutional addresses.
        }
        throw new InvalidRegistrationException("Une adresse institutionnelle @usms.ac.ma est requise.");
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRegistrationException("Le mot de passe dépasse 72 octets UTF-8.");
        }
    }
}
