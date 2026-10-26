package ma.ensa.smartcampus.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.ensa.smartcampus.model.User;
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
        when(users.findByEmail("etudiant@usms.ac.ma")).thenReturn(Optional.of(
                new User(1L, "etudiant@usms.ac.ma", passwordEncoder.encode("Passw0rd!2026"), "STUDENT")));
    }

    @Test
    public void testLoginSuccess() {
        compteExistant();
        assertTrue(loginService.authenticate("etudiant@usms.ac.ma", "Passw0rd!2026").isPresent());
    }

    @Test
    public void testLoginFailureWrongPassword() {
        compteExistant();
        assertFalse(loginService.authenticate("etudiant@usms.ac.ma", "MotDePasseFaux").isPresent());
    }

    @Test
    public void testLoginFailureWrongDomain() {
        assertFalse(loginService.authenticate("etudiant@gmail.com", "Passw0rd!2026").isPresent());
    }

    @Test
    public void testLoginIgnoreLaCasseDeLEmail() {
        compteExistant();
        assertTrue(loginService.authenticate("Etudiant@USMS.AC.MA", "Passw0rd!2026").isPresent());
    }
}
