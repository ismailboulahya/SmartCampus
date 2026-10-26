package ma.ensa.smartcampus.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import ma.ensa.smartcampus.user.UserInfo;

/** Réponse de POST /api/login. token, expiresAt et user ne sont présents qu'en cas de succès. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResult(
        boolean success,
        String message,
        String token,
        Instant expiresAt,
        UserInfo user,
        @JsonIgnore LoginStatus status) {

    public static LoginResult success(String token, Instant expiresAt, UserInfo user) {
        return new LoginResult(true, "Connexion réussie.", token, expiresAt, user, LoginStatus.SUCCESS);
    }

    public static LoginResult failure(LoginStatus status, String message) {
        return new LoginResult(false, message, null, null, null, status);
    }
}
