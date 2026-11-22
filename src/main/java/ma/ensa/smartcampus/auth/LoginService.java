package ma.ensa.smartcampus.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private static final String DOMAINE_INSTITUTIONNEL = "@usms.ac.ma";
    private static final int MAX_ECHECS_AVANT_VERROUILLAGE = 5;
    private static final Duration DUREE_DU_VERROUILLAGE = Duration.ofMinutes(15);

    public enum Statut {
        SUCCES,
        IDENTIFIANTS_INVALIDES,
        COMPTE_VERROUILLE
    }

    public record Resultat(Statut statut, User utilisateur) {

        public static Resultat succes(User utilisateur) {
            return new Resultat(Statut.SUCCES, utilisateur);
        }

        public static Resultat identifiantsInvalides() {
            return new Resultat(Statut.IDENTIFIANTS_INVALIDES, null);
        }

        public static Resultat compteVerrouille(User utilisateur) {
            return new Resultat(Statut.COMPTE_VERROUILLE, utilisateur);
        }
    }

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public Resultat authentifier(String email, String password) {
        if (email == null || password == null) {
            return Resultat.identifiantsInvalides();
        }
        String normalise = normaliser(email);
        if (!normalise.endsWith(DOMAINE_INSTITUTIONNEL)) {
            return Resultat.identifiantsInvalides();
        }
        Optional<User> trouve = users.findByEmail(normalise);
        if (trouve.isEmpty()) {
            return Resultat.identifiantsInvalides();
        }

        User utilisateur = trouve.get();
        LocalDateTime maintenant = LocalDateTime.now();

        if (utilisateur.isVerrouille() && !utilisateur.estVerrouille(maintenant)) {
            // Le verrouillage a expiré : l'utilisateur repart avec un compteur vierge.
            utilisateur.reinitialiserLesTentatives();
        }
        if (utilisateur.estVerrouille(maintenant)) {
            return Resultat.compteVerrouille(utilisateur);
        }
        if (!passwordEncoder.matches(password, utilisateur.getMotDePasseHash())) {
            utilisateur.enregistrerEchecDeConnexion(MAX_ECHECS_AVANT_VERROUILLAGE, DUREE_DU_VERROUILLAGE, maintenant);
            users.save(utilisateur);
            return utilisateur.estVerrouille(maintenant)
                    ? Resultat.compteVerrouille(utilisateur)
                    : Resultat.identifiantsInvalides();
        }

        if (utilisateur.getNbrConnexionEchouee() > 0 || utilisateur.isVerrouille()) {
            utilisateur.reinitialiserLesTentatives();
            users.save(utilisateur);
        }
        return Resultat.succes(utilisateur);
    }

    public static String normaliser(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
