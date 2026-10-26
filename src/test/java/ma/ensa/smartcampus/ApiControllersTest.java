package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ma.ensa.smartcampus.auth.LoginResult;
import ma.ensa.smartcampus.auth.LoginService;
import ma.ensa.smartcampus.auth.LoginStatus;
import ma.ensa.smartcampus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiControllersTest {

    @Test
    void healthRetourneUP() {
        StatusController controller = new StatusController(null);
        assertEquals("UP", controller.health().get("status"));
    }

    @Test
    void loginAvecMauvaisMotDePasseRetourne401() {
        LoginService loginService = mock(LoginService.class);
        when(loginService.login(eq("inconnu@usms.ac.ma"), eq("mauvais-mot-de-passe"), any()))
                .thenReturn(LoginResult.failure(LoginStatus.UNKNOWN_USER, "Identifiants invalides."));
        AuthController controller = new AuthController(loginService, mock(UserRepository.class));

        var response = controller.login(
                new AuthController.LoginRequest("inconnu@usms.ac.ma", "mauvais-mot-de-passe"),
                new MockHttpServletRequest());

        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }
}
