package ma.ensa.smartcampus.exception;

import java.util.Map;
import java.util.Objects;
import ma.ensa.smartcampus.dto.ApiMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountDecisionException.class)
    public ResponseEntity<ApiMessage> decisionConflict(AccountDecisionException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiMessage(false, exception.getMessage()));
    }

    @ExceptionHandler(InvalidRegistrationException.class)
    public ResponseEntity<ApiMessage> invalidRegistration(InvalidRegistrationException exception) {
        return ResponseEntity.badRequest().body(new ApiMessage(false, exception.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiMessage> invalidCredentials() {
        return ResponseEntity.status(401).body(new ApiMessage(false, "Identifiants invalides."));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiMessage> locked() {
        return ResponseEntity.status(423).body(new ApiMessage(false, "Compte temporairement verrouillé."));
    }

    @ExceptionHandler(LoginLimitExceededException.class)
    public ResponseEntity<ApiMessage> tooManyAttempts() {
        return ResponseEntity.status(429).body(new ApiMessage(false, "Trop de tentatives, réessayez plus tard."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getDefaultMessage())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Requête invalide.");
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Requête invalide."));
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ResponseEntity<Map<String, Object>> handleRequeteInvalide(RequeteInvalideException exception) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<Map<String, Object>> handleRessourceIntrouvable(RessourceIntrouvableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", exception.getMessage()));
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<Map<String, Object>> handleConflit(ConflitException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", exception.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handlePersistence(DataAccessException exception) {
        LOG.error("Erreur de persistance", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Erreur de base de données, réessayez plus tard."));
    }
}
