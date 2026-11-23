package ma.ensa.smartcampus.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AccountDecisionException extends RuntimeException {
    private final HttpStatus status;

    public AccountDecisionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
