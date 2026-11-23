package ma.ensa.smartcampus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record RegistrationRequest(@NotNull Profile role, @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password, @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName, @Valid Student studentProfile, @Valid Teacher teacherProfile) {

    public enum Profile { STUDENT, TEACHER }

    public record Student(@NotBlank @Size(max = 255) String cne, @NotBlank @Size(max = 255) String filiere) {}
    public record Teacher(@NotBlank @Size(max = 255) String matricule, @NotBlank @Size(max = 255) String departement) {}
}
