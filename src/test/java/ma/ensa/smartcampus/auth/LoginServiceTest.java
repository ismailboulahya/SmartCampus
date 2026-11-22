package ma.ensa.smartcampus.auth;

import static ma.ensa.smartcampus.auth.LoginService.Statut.COMPTE_VERROUILLE;
import static ma.ensa.smartcampus.auth.LoginService.Statut.IDENTIFIANTS_INVALIDES;
import static ma.ensa.smartcampus.auth.LoginService.Statut.SUCCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final String EMAIL = "souschef@usms.ac.ma";
    private static final String MOT_DE_PASSE = "SousChef!2026";

    private final UserRepository users = Mockito.mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final LoginService loginService = new LoginService(users, passwordEncoder);
    private final User compte = new User("Youssef", "Kabbaj", EMAIL, passwordEncoder.encode(MOT_DE_PASSE),
            UserRole.SOUS_CHEF, 1003L, 1);

    private void compteExistant() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(compte));
    }

    @Test
    public void testLoginSuccess() {
        compteExistant();
        assertEquals(SUCCES, loginService.authentifier(EMAIL, MOT_DE_PASSE).statut());
    }

    @Test
    public void testLoginFailureWrongPassword() {
        compteExistant();
        assertEquals(IDENTIFIANTS_INVALIDES, loginService.authentifier(EMAIL, "MotDePasseFaux").statut());
    }

    @Test
    public void testLoginFailureWrongDomain() {
        assertEquals(IDENTIFIANTS_INVALIDES, loginService.authentifier("souschef@gmail.com", MOT_DE_PASSE).statut());
    }

    @Test
    public void testLoginIgnoreLaCasseDeLEmail() {
        compteExistant();
        assertEquals(SUCCES, loginService.authentifier("SousChef@USMS.AC.MA", MOT_DE_PASSE).statut());
    }

    @Test
    public void testVerrouillageApresCinqEchecs() {
        compteExistant();
        for (int tentative = 1; tentative <= 4; tentative++) {
            assertEquals(IDENTIFIANTS_INVALIDES, loginService.authentifier(EMAIL, "MotDePasseFaux").statut());
        }
        assertEquals(COMPTE_VERROUILLE, loginService.authentifier(EMAIL, "MotDePasseFaux").statut());
        assertTrue(compte.isLocked());
        assertEquals(5, compte.getFailedLoginAttempts());
    }

    @Test
    public void testCompteVerrouilleRefuseLeBonMotDePasse() {
        compteExistant();
        for (int tentative = 1; tentative <= 5; tentative++) {
            loginService.authentifier(EMAIL, "MotDePasseFaux");
        }
        assertEquals(COMPTE_VERROUILLE, loginService.authentifier(EMAIL, MOT_DE_PASSE).statut());
    }
}
