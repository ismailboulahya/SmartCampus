package ma.ensa.smartcampus.auth;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import ma.ensa.smartcampus.security.JwtService;
import ma.ensa.smartcampus.user.User;
import ma.ensa.smartcampus.user.UserInfo;
import ma.ensa.smartcampus.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentification par email institutionnel + mot de passe (BCrypt),
 * avec verrouillage après plusieurs échecs et émission d'un JWT.
 */
@Service
public class LoginService {

    public static final String DOMAINE_INSTITUTIONNEL = "@usms.ac.ma";

    static final String MSG_INVALIDES = "Identifiants invalides.";
    static final String MSG_DESACTIVE = "Compte désactivé. Contactez l'administration.";
    static final String MSG_VERROUILLE = "Compte temporairement verrouillé. Réessayez plus tard.";

    private final UserRepository users;
    private final LoginAuditRepository audit;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final Clock clock;

    /** Hash factice : évite qu'un email inconnu réponde plus vite qu'un mauvais mot de passe. */
    private final String dummyHash;

    public LoginService(UserRepository users, LoginAuditRepository audit, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AuthProperties properties, Clock clock) {
        this.users = users;
        this.audit = audit;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("smartcampus-dummy-password");
    }

    /** Conservé pour compatibilité avec l'ancienne API (SC-11). */
    public boolean authenticate(String email, String password) {
        return login(email, password, null).success();
    }

    @Transactional
    public LoginResult login(String rawEmail, String password, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String email = normalize(rawEmail);

        if (email.isEmpty() || password == null || password.isEmpty()) {
            return fail(email, null, LoginStatus.INVALID_INPUT, MSG_INVALIDES, ipAddress, now);
        }
        if (!email.endsWith(DOMAINE_INSTITUTIONNEL)) {
            return fail(email, null, LoginStatus.INVALID_DOMAIN, MSG_INVALIDES, ipAddress, now);
        }

        Optional<User> found = users.findByEmail(email);
        if (found.isEmpty()) {
            passwordEncoder.matches(password, dummyHash);
            return fail(email, null, LoginStatus.UNKNOWN_USER, MSG_INVALIDES, ipAddress, now);
        }

        User user = found.get();
        if (user.isLockedAt(now)) {
            return fail(email, user.id(), LoginStatus.LOCKED, MSG_VERROUILLE, ipAddress, now);
        }
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            users.recordFailedAttempt(user.id(), properties.maxFailedAttempts(),
                    now.plus(properties.lockDuration()), now);
            boolean nowLocked = user.failedAttempts() + 1 >= properties.maxFailedAttempts();
            return nowLocked
                    ? fail(email, user.id(), LoginStatus.LOCKED, MSG_VERROUILLE, ipAddress, now)
                    : fail(email, user.id(), LoginStatus.BAD_PASSWORD, MSG_INVALIDES, ipAddress, now);
        }
        if (!user.enabled()) {
            return fail(email, user.id(), LoginStatus.DISABLED, MSG_DESACTIVE, ipAddress, now);
        }

        users.recordSuccessfulLogin(user.id(), now);
        audit.record(email, user.id(), LoginStatus.SUCCESS, ipAddress, now);
        JwtService.IssuedToken token = jwtService.generate(user);
        return LoginResult.success(token.value(), token.expiresAt(), UserInfo.from(user));
    }

    private LoginResult fail(String email, Long userId, LoginStatus status, String message,
                             String ipAddress, OffsetDateTime now) {
        audit.record(email, userId, status, ipAddress, now);
        return LoginResult.failure(status, message);
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
