package ma.ensa.smartcampus.exception;

/**
 * Levée lorsqu'une opération entre en conflit avec une donnée déjà existante.
 */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
