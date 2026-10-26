package ma.ensa.smartcampus.auth;

/** Issue d'une tentative de connexion (aussi enregistrée dans login_audit.reason). */
public enum LoginStatus {
    SUCCESS(200),
    INVALID_INPUT(401),
    INVALID_DOMAIN(401),
    UNKNOWN_USER(401),
    BAD_PASSWORD(401),
    DISABLED(403),
    LOCKED(423);

    private final int httpStatus;

    LoginStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
