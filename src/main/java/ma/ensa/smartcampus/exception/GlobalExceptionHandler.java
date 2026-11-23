package ma.ensa.smartcampus.exception;

import ma.ensa.smartcampus.dto.ApiMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(LoginLimitExceededException.class)
    public ResponseEntity<ApiMessage> tooManyAttempts() {
        return ResponseEntity.status(429)
                .body(new ApiMessage(false, "Trop de tentatives, réessayez plus tard."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessage> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiMessage(false, "Requête invalide."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiMessage> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiMessage(false, "Requête invalide."));
    }
}
