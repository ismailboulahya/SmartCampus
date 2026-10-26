package ma.ensa.smartcampus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import ma.ensa.smartcampus.auth.AuthProperties;
import ma.ensa.smartcampus.user.Role;
import ma.ensa.smartcampus.user.User;
import org.springframework.stereotype.Service;

/** Création et vérification des JWT (HMAC-SHA256). */
@Service
public class JwtService {

    static final int MIN_SECRET_LENGTH = 32;
    private static final String ISSUER = "smartcampus";

    public record IssuedToken(String value, Instant expiresAt) {}

    private final SecretKey key;
    private final AuthProperties properties;
    private final Clock clock;

    public JwtService(AuthProperties properties, Clock clock) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET manquant ou trop court : au moins " + MIN_SECRET_LENGTH
                    + " caractères requis (ou lancer avec SPRING_PROFILES_ACTIVE=dev).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken generate(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.jwtExpiration());
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject(user.email())
                .claim("uid", user.id())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Retourne l'utilisateur si le token est valide (signature, émetteur, expiration), sinon vide. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number uid = claims.get("uid", Number.class);
            String role = claims.get("role", String.class);
            if (uid == null || role == null || claims.getSubject() == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(uid.longValue(), claims.getSubject(), Role.valueOf(role)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
