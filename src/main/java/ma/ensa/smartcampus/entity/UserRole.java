package ma.ensa.smartcampus.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    STUDENT(Etudiant.class),
    TEACHER(Enseignant.class),
    ADMIN(Administrateur.class);

    private final Class<? extends Utilisateur> entityType;
}
