package ma.ensa.smartcampus.dto;

import java.time.Instant;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;

public record AccountDetails(boolean success, Long id, String email, UserRole role, AccountStatus status,
        String firstName, String lastName, Instant createdAt, Student studentProfile, Teacher teacherProfile) {

    public record Student(String cne, String filiere) {}
    public record Teacher(String matricule, String departement) {}
}
