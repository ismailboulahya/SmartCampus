package ma.ensa.smartcampus.exception;

/**
 * Levée lorsqu'une ressource demandée n'existe pas.
 */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
