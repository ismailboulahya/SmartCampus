package ma.ensa.smartcampus.model;

/**
 * Rôle applicatif utilisé par Spring Security. Il correspond au type concret de l'utilisateur.
 */
public enum UserRole {
    ETUDIANT,
    PROFESSEUR,
    ADMIN,
    SUPERADMIN
}
