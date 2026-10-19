package ma.ensa.smartcampus.auth;

import java.util.Map;

public class LoginService {
    private static final String DOMAINE_INSTITUTIONNEL = "@usms.ac.ma";

    // Comptes simulés
    private final Map<String, String> comptes =
            Map.of("etudiant@usms.ac.ma", "Passw0rd!2026");

    public boolean authenticate(String email, String password) {
        if (email == null || password == null
                || !email.endsWith(DOMAINE_INSTITUTIONNEL)) {
            return false;
        }
        return password.equals(comptes.get(email));
    }
}