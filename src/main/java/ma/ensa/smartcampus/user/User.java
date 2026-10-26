package ma.ensa.smartcampus.user;

import java.time.OffsetDateTime;

/** Ligne de la table users. */
public record User(
        long id,
        String email,
        String passwordHash,
        String firstName,
        String lastName,
        Role role,
        boolean enabled,
        int failedAttempts,
        OffsetDateTime lockedUntil,
        OffsetDateTime lastLoginAt) {

    public boolean isLockedAt(OffsetDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
