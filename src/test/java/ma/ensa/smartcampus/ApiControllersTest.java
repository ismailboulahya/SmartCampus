package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ma.ensa.smartcampus.auth.LoginService;
import org.junit.jupiter.api.Test;

class ApiControllersTest {

    @Test
    void healthRetourneUP() {
        StatusController controller = new StatusController(null);
        assertEquals("UP", controller.health().get("status"));
    }

    @Test
    void loginAvecMauvaisMotDePasseRetourne401() {
        AuthController controller = new AuthController(new LoginService());
        var response = controller.login(
                new AuthController.LoginRequest("inconnu@usms.ac.ma", "mauvais-mot-de-passe"));
        assertEquals(401, response.getStatusCode().value());
    }
}
