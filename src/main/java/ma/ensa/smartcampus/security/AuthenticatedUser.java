package ma.ensa.smartcampus.security;

import ma.ensa.smartcampus.user.Role;

/** Utilisateur extrait d'un JWT valide (principal Spring Security). */
public record AuthenticatedUser(long id, String email, Role role) {
}
