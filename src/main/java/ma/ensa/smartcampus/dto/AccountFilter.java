package ma.ensa.smartcampus.dto;

import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;

public record AccountFilter(UserRole role, AccountStatus status, String email) {}
