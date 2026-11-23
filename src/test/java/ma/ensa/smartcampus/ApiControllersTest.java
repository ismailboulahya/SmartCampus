package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import ma.ensa.smartcampus.controller.AuthController;
import ma.ensa.smartcampus.controller.StatusController;
import ma.ensa.smartcampus.dto.LoginRequest;
import ma.ensa.smartcampus.dto.LoginResponse;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.service.AccountService;
import ma.ensa.smartcampus.service.LoginService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiControllersTest {

    @Test
    void healthRetourneUP() {
        assertEquals("UP", new StatusController(null).health().status());
    }

    @Test
    void loginRetourneLaReponseDuService() {
        LoginService loginService = Mockito.mock(LoginService.class);
        var controller = new AuthController(loginService, Mockito.mock(AccountService.class));
        var request = new LoginRequest("etudiant@usms.ac.ma", "secret");
        var expected = new LoginResponse(true, "token", UserRole.STUDENT);
        when(loginService.login(request.email(), request.password())).thenReturn(expected);

        assertEquals(expected, controller.login(request));
    }
}
