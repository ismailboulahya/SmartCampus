package ma.ensa.smartcampus.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import ma.ensa.smartcampus.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    /** HS256 exige une clé d'au moins 256 bits. */
    private static final int LONGUEUR_MINIMALE_DU_SECRET = 32;

    private final SecretKey key;
    private final long expiration;

    public JwtService(@Value("${app.jwt.secret:}") String secret, @Value("${app.jwt.expiration}") long expiration) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(StandardCharsets.UTF_8).length < LONGUEUR_MINIMALE_DU_SECRET) {
            throw new IllegalStateException(
                    "La variable d'environnement JWT_SECRET est absente ou trop courte (32 caractères minimum).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generate(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
