package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.auth.LoginRateLimiter;
import ma.ensa.smartcampus.auth.LoginService;
import ma.ensa.smartcampus.controller.AuthController;
import ma.ensa.smartcampus.controller.StatusController;
import ma.ensa.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiControllersTest {

    private final LoginService loginService = Mockito.mock(LoginService.class);
    private final UserRepository users = Mockito.mock(UserRepository.class);
    private final JwtService jwtService = new JwtService("smartcampus-test-secret-key-1234567890abcdef", 3600000L);
    private final LoginRateLimiter rateLimiter = new LoginRateLimiter();
    private final AuthController controller =
            new AuthController(loginService, jwtService, rateLimiter, users);

    @Test
    void healthRetourneUP() {
        StatusController statusController = new StatusController(null);
        assertEquals("UP", statusController.health().get("status"));
    }

    @Test
    void loginAvecMauvaisMotDePasseRetourne401() {
        when(loginService.authentifier("inconnu@usms.ac.ma", "mauvais-mot-de-passe"))
                .thenReturn(LoginService.Resultat.identifiantsInvalides());
        var response = controller.login(new AuthController.LoginRequest("inconnu@usms.ac.ma", "mauvais-mot-de-passe"));
        assertEquals(401, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("success"));
    }

    @Test
    void loginSurCompteVerrouilleRetourne423() {
        when(loginService.authentifier("verrouille@usms.ac.ma", "SousChef!2026"))
                .thenReturn(LoginService.Resultat.compteVerrouille(null));
        var response = controller.login(new AuthController.LoginRequest("verrouille@usms.ac.ma", "SousChef!2026"));
        assertEquals(423, response.getStatusCode().value());
        assertFalse((Boolean) response.getBody().get("success"));
    }
}
