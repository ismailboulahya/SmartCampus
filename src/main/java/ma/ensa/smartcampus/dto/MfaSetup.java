package ma.ensa.smartcampus.dto;

import java.time.Instant;

public record MfaSetup(String setupToken, String manualKey, String authenticatorUri, Instant expiresAt) {}
