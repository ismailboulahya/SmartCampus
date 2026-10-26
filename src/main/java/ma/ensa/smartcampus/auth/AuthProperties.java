package ma.ensa.smartcampus.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Paramètres smartcampus.auth.* de application.properties. */
@ConfigurationProperties(prefix = "smartcampus.auth")
public record AuthProperties(
        String jwtSecret,
        @DefaultValue("1h") Duration jwtExpiration,
        @DefaultValue("5") int maxFailedAttempts,
        @DefaultValue("15m") Duration lockDuration) {
}
