package ma.ensa.smartcampus.dto;

import ma.ensa.smartcampus.entity.UserRole;

public record LoginResponse(boolean success, String token, UserRole role) {}
