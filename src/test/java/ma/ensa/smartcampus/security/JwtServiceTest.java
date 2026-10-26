package ma.ensa.smartcampus.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import ma.ensa.smartcampus.auth.AuthProperties;
import ma.ensa.smartcampus.user.Role;
import ma.ensa.smartcampus.user.User;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "secret-de-test-0123456789-0123456789";
    private static final Instant T0 = Instant.parse("2026-10-26T10:00:00Z");
    private static final User ADMIN =
            new User(7L, "admin@usms.ac.ma", "hash", "Admin", "Test", Role.ADMIN, true, 0, null, null);

    private static JwtService service(String secret, Instant now) {
        return new JwtService(new AuthProperties(secret, Duration.ofHours(1), 5, Duration.ofMinutes(15)),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void tokenGenereEstRelu() {
        JwtService jwt = service(SECRET, T0);
        JwtService.IssuedToken token = jwt.generate(ADMIN);

        AuthenticatedUser user = jwt.parse(token.value()).orElseThrow();
        assertEquals(7L, user.id());
        assertEquals("admin@usms.ac.ma", user.email());
        assertEquals(Role.ADMIN, user.role());
        assertEquals(T0.plus(Duration.ofHours(1)), token.expiresAt());
    }

    @Test
    void tokenExpireRefuse() {
        String token = service(SECRET, T0).generate(ADMIN).value();
        assertTrue(service(SECRET, T0.plus(Duration.ofHours(2))).parse(token).isEmpty());
    }

    @Test
    void tokenSigneAvecUnAutreSecretRefuse() {
        String token = service("un-autre-secret-0123456789-0123456789", T0).generate(ADMIN).value();
        assertTrue(service(SECRET, T0).parse(token).isEmpty());
    }

    @Test
    void tokenMalformeRefuse() {
        assertTrue(service(SECRET, T0).parse("pas.un.jwt").isEmpty());
        assertTrue(service(SECRET, T0).parse("").isEmpty());
    }

    @Test
    void secretTropCourtBloqueLeDemarrage() {
        assertThrows(IllegalStateException.class, () -> service("court", T0));
        assertThrows(IllegalStateException.class, () -> service(null, T0));
    }
}
