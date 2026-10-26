package ma.ensa.smartcampus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import ma.ensa.smartcampus.security.JwtService;
import ma.ensa.smartcampus.user.Role;
import ma.ensa.smartcampus.user.User;
import ma.ensa.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final Instant NOW_INSTANT = Instant.parse("2026-10-26T10:00:00Z");
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(NOW_INSTANT, ZoneOffset.UTC);
    private static final String EMAIL = "etudiant@usms.ac.ma";
    private static final String PASSWORD = "Passw0rd!2026";

    // Coût BCrypt minimal pour garder les tests rapides
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);
    private static final String HASH = ENCODER.encode(PASSWORD);

    @Mock UserRepository users;
    @Mock LoginAuditRepository audit;

    private final AuthProperties props = new AuthProperties(
            "secret-de-test-0123456789-0123456789", Duration.ofHours(1), 5, Duration.ofMinutes(15));
    private final Clock clock = Clock.fixed(NOW_INSTANT, ZoneOffset.UTC);

    private JwtService jwtService;
    private LoginService loginService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(props, clock);
        loginService = new LoginService(users, audit, ENCODER, jwtService, props, clock);
    }

    private static User student(int failedAttempts, OffsetDateTime lockedUntil, boolean enabled) {
        return new User(1L, EMAIL, HASH, "Etudiant", "Test", Role.STUDENT,
                enabled, failedAttempts, lockedUntil, null);
    }

    @Test
    void testLoginSuccess() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, true)));

        LoginResult result = loginService.login(EMAIL, PASSWORD, "10.0.0.1");

        assertTrue(result.success(), "La connexion devrait réussir avec les bons identifiants.");
        assertEquals(LoginStatus.SUCCESS, result.status());
        assertNotNull(result.token());
        assertEquals(EMAIL, result.user().email());
        assertEquals(Role.STUDENT, jwtService.parse(result.token()).orElseThrow().role());
        verify(users).recordSuccessfulLogin(1L, NOW);
        verify(audit).record(EMAIL, 1L, LoginStatus.SUCCESS, "10.0.0.1", NOW);
    }

    @Test
    void testLoginFailureWrongPassword() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, true)));

        LoginResult result = loginService.login(EMAIL, "MotDePasseFaux", null);

        assertFalse(result.success(), "La connexion devrait échouer avec un mauvais mot de passe.");
        assertEquals(LoginStatus.BAD_PASSWORD, result.status());
        assertNull(result.token());
        verify(users).recordFailedAttempt(1L, 5, NOW.plusMinutes(15), NOW);
        verify(users, never()).recordSuccessfulLogin(anyLong(), any());
    }

    @Test
    void testLoginFailureWrongDomain() {
        LoginResult result = loginService.login("etudiant@gmail.com", PASSWORD, null);

        assertFalse(result.success(), "La connexion devrait échouer si le domaine n'est pas @usms.ac.ma.");
        assertEquals(LoginStatus.INVALID_DOMAIN, result.status());
        verifyNoInteractions(users);
    }

    @Test
    void emailInconnuDonneLeMemeMessageQuUnMauvaisMotDePasse() {
        when(users.findByEmail("inconnu@usms.ac.ma")).thenReturn(Optional.empty());

        LoginResult result = loginService.login("inconnu@usms.ac.ma", PASSWORD, null);

        assertEquals(LoginStatus.UNKNOWN_USER, result.status());
        assertEquals(LoginService.MSG_INVALIDES, result.message());
        verify(audit).record("inconnu@usms.ac.ma", null, LoginStatus.UNKNOWN_USER, null, NOW);
    }

    @Test
    void emailNormaliseAvantRecherche() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, true)));

        assertTrue(loginService.login("  Etudiant@USMS.ac.ma ", PASSWORD, null).success());
    }

    @Test
    void champsVidesRefuses() {
        assertEquals(LoginStatus.INVALID_INPUT, loginService.login(null, PASSWORD, null).status());
        assertEquals(LoginStatus.INVALID_INPUT, loginService.login(EMAIL, "", null).status());
        verifyNoInteractions(users);
    }

    @Test
    void compteVerrouilleRefuseMemeAvecLeBonMotDePasse() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, NOW.plusMinutes(5), true)));

        LoginResult result = loginService.login(EMAIL, PASSWORD, null);

        assertEquals(LoginStatus.LOCKED, result.status());
        verify(users, never()).recordSuccessfulLogin(anyLong(), any());
        verify(users, never()).recordFailedAttempt(anyLong(), anyInt(), any(), any());
    }

    @Test
    void verrouExpireAutoriseLaConnexion() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, NOW.minusMinutes(1), true)));

        assertTrue(loginService.login(EMAIL, PASSWORD, null).success());
    }

    @Test
    void cinquiemeEchecVerrouilleLeCompte() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(4, null, true)));

        LoginResult result = loginService.login(EMAIL, "MotDePasseFaux", null);

        assertEquals(LoginStatus.LOCKED, result.status());
        verify(users).recordFailedAttempt(1L, 5, NOW.plusMinutes(15), NOW);
    }

    @Test
    void compteDesactiveRefuse() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, false)));

        assertEquals(LoginStatus.DISABLED, loginService.login(EMAIL, PASSWORD, null).status());
    }

    @Test
    void compteDesactiveAvecMauvaisMotDePasseNeRevelePasLEtat() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, false)));

        assertEquals(LoginStatus.BAD_PASSWORD, loginService.login(EMAIL, "MotDePasseFaux", null).status());
    }

    @Test
    void authenticateResteCompatible() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(student(0, null, true)));

        assertTrue(loginService.authenticate(EMAIL, PASSWORD));
    }
}
