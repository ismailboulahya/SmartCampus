package ma.ensa.smartcampus.dto;

import jakarta.validation.constraints.NotNull;
import ma.ensa.smartcampus.entity.AccountStatus;

public record AccountStatusChange(@NotNull AccountStatus from, @NotNull AccountStatus to) {}
