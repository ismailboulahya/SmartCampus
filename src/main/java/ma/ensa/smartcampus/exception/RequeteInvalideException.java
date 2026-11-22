package ma.ensa.smartcampus.exception;

/**
 * Levée lorsqu'une donnée fournie ne respecte pas les règles de gestion.
 */
public class RequeteInvalideException extends RuntimeException {

    public RequeteInvalideException(String message) {
        super(message);
    }
}
