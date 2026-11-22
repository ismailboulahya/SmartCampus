package ma.ensa.smartcampus.auth;

import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * Limite les tentatives de connexion par adresse email sur une fenêtre glissante.
 * Les clefs inactives sont évincées pour éviter de faire grossir la map indéfiniment.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TENTATIVES = 5;
    private static final long FENETRE_MS = 60_000L;

    private final Map<String, Deque<Long>> tentatives = new ConcurrentHashMap<>();

    public boolean estBloque(String email) {
        Deque<Long> horodatages = tentatives.get(clef(email));
        if (horodatages == null) {
            return false;
        }
        purgerLesTentativesExpirees(horodatages);
        return horodatages.size() >= MAX_TENTATIVES;
    }

    public void enregistrerEchec(String email) {
        long maintenant = System.currentTimeMillis();
        tentatives.computeIfAbsent(clef(email), k -> new ConcurrentLinkedDeque<>()).addLast(maintenant);
        purgerLesClesInactives(maintenant);
    }

    public void reinitialiser(String email) {
        tentatives.remove(clef(email));
    }

    private static String clef(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void purgerLesTentativesExpirees(Deque<Long> horodatages) {
        long limite = System.currentTimeMillis() - FENETRE_MS;
        Long plusAncienne;
        while ((plusAncienne = horodatages.peekFirst()) != null && plusAncienne < limite) {
            horodatages.pollFirst();
        }
    }

    private void purgerLesClesInactives(long maintenant) {
        tentatives.entrySet().removeIf(entree -> {
            Long plusRecente = entree.getValue().peekLast();
            return plusRecente == null || maintenant - plusRecente > FENETRE_MS;
        });
    }
}
