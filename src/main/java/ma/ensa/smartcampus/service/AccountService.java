package ma.ensa.smartcampus.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import ma.ensa.smartcampus.dto.AccountDetails;
import ma.ensa.smartcampus.dto.AccountFilter;
import ma.ensa.smartcampus.dto.AccountSummary;
import ma.ensa.smartcampus.dto.AccountStatusChange;
import ma.ensa.smartcampus.exception.AccountDecisionException;
import org.springframework.http.HttpStatus;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.Administrateur;
import ma.ensa.smartcampus.entity.Enseignant;
import ma.ensa.smartcampus.entity.Etudiant;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.repository.specification.UserSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final UserRepository users;

    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    static UserRole roleOf(Class<? extends Utilisateur> type) {
        for (UserRole role : UserRole.values()) {
            if (role.getEntityType().isAssignableFrom(type)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Type d’utilisateur inconnu : " + type.getName());
    }

    public Optional<UserRole> activeRole(String email) {
        return users.findTypeByEmailAndStatus(email, AccountStatus.ACTIF).map(AccountService::roleOf);
    }

    public AccountDetails currentAccount(String email) {
        Utilisateur user = users.findByEmailAndStatus(email, AccountStatus.ACTIF)
                .orElseThrow(() -> new BadCredentialsException("Compte indisponible."));
        var student = user instanceof Etudiant e ? new AccountDetails.Student(e.getCne(), e.getFiliere()) : null;
        var teacher = user instanceof Enseignant e ? new AccountDetails.Teacher(e.getMatricule(), e.getDepartement()) : null;
        return new AccountDetails(true, user.getId(), user.getEmail(), roleOf(user.getClass()), user.getStatus(),
                user.getFirstName(), user.getLastName(), user.getCreatedAt(), student, teacher);
    }

    public List<AccountSummary> registrationsReadyForReview() {
        return summaries(UserSpecifications.readyForReview());
    }

    public List<AccountSummary> listAccounts(AccountFilter filter) {
        String email = filter.email();
        String normalized = email == null || email.isBlank() ? null : normalizeEmail(email);
        var criteria = UserSpecifications.matching(filter.role(), filter.status(), normalized);
        return summaries(criteria);
    }

    private List<AccountSummary> summaries(Specification<Utilisateur> criteria) {
        return users.findAll(criteria, Sort.by("id")).stream()
                .map(this::summary)
                .toList();
    }

    private AccountSummary summary(Utilisateur user) {
        return new AccountSummary(user.getId(), user.getEmail(), roleOf(user.getClass()), user.getStatus());
    }

    @Transactional
    public AccountSummary changeStatus(long id, AccountStatusChange change, String administrator) {
        var locked = users.lockForDecision(id, administrator);
        boolean authorized = locked.stream().anyMatch(u -> u instanceof Administrateur
                && u.getEmail().equals(administrator) && u.getStatus() == AccountStatus.ACTIF);
        if (!authorized) {
            throw new AccountDecisionException(HttpStatus.FORBIDDEN, "Accès administrateur requis.");
        }
        var user = locked.stream().filter(u -> u.getId() == id).findFirst().orElseThrow(() ->
                new AccountDecisionException(HttpStatus.NOT_FOUND, "Compte introuvable."));
        if (user.getEmail().equals(administrator)) {
            throw new AccountDecisionException(HttpStatus.CONFLICT, "Vous ne pouvez pas changer votre propre statut.");
        }
        if (user.getStatus() != change.from()) {
            throw new AccountDecisionException(HttpStatus.CONFLICT, "Le statut a déjà changé. Actualisez la liste.");
        }
        boolean allowed = switch (user.getStatus()) {
            case EN_ATTENTE -> (change.to() == AccountStatus.ACTIF || change.to() == AccountStatus.REFUSE)
                    && user.getEmailVerifiedAt() != null && user.getMfaActivatedAt() != null;
            case ACTIF -> change.to() == AccountStatus.BLOQUE;
            case BLOQUE -> change.to() == AccountStatus.ACTIF;
            case REFUSE -> false;
        };
        if (!allowed) {
            throw new AccountDecisionException(HttpStatus.CONFLICT, "Ce dossier ne permet pas cette décision.");
        }
        user.setStatus(change.to());
        return summary(user);
    }
}
