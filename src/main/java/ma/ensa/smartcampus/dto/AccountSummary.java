package ma.ensa.smartcampus.dto;

import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;

public record AccountSummary(Long id, String email, UserRole role, AccountStatus status) {}
