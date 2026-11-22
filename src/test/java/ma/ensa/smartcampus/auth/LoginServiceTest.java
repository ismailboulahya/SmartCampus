package ma.ensa.smartcampus.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.model.UserRole;
import ma.ensa.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class LoginServiceTest {

    private final UserRepository users = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final LoginService loginService = new LoginService(users, passwordEncoder);

    private void compteExistant() {
        when(users.findByEmail("souschef@usms.ac.ma")).thenReturn(Optional.of(
                new User("Youssef", "Kabbaj", "souschef@usms.ac.ma",
                        passwordEncoder.encode("SousChef!2026"), UserRole.SOUS_CHEF, 1003L, 1)));
    }

    @Test
    public void testLoginSuccess() {
        compteExistant();
        assertTrue(loginService.authenticate("souschef@usms.ac.ma", "SousChef!2026").isPresent());
    }

    @Test
    public void testLoginFailureWrongPassword() {
        compteExistant();
        assertFalse(loginService.authenticate("souschef@usms.ac.ma", "MotDePasseFaux").isPresent());
    }

    @Test
    public void testLoginFailureWrongDomain() {
        assertFalse(loginService.authenticate("souschef@gmail.com", "SousChef!2026").isPresent());
    }

    @Test
    public void testLoginIgnoreLaCasseDeLEmail() {
        compteExistant();
        assertTrue(loginService.authenticate("SousChef@USMS.AC.MA", "SousChef!2026").isPresent());
    }
}
