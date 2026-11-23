package ma.ensa.smartcampus.repository.specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.entity.Utilisateur;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<Utilisateur> readyForReview() {
        return (root, query, cb) -> cb.and(cb.equal(root.get("status"), AccountStatus.EN_ATTENTE),
                cb.isNotNull(root.get("emailVerifiedAt")), cb.isNotNull(root.get("mfaActivatedAt")));
    }

    public static Specification<Utilisateur> matching(UserRole role, AccountStatus status, String email) {
        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (role != null) {
                conditions.add(builder.equal(root.type(), role.getEntityType()));
            }
            if (status != null) {
                conditions.add(builder.equal(root.get("status"), status));
            }
            if (email != null) {
                conditions.add(builder.equal(root.get("email"), email));
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }
}
