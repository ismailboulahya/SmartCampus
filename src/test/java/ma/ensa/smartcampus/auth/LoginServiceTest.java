package ma.ensa.smartcampus.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import ma.ensa.smartcampus.entity.*;
import ma.ensa.smartcampus.exception.LoginLimitExceededException;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.service.AccountCreationService;
import ma.ensa.smartcampus.service.LoginService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LoginServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService tokens = mock(JwtService.class);
    private final LoginService service = new LoginService(users, encoder, new LoginRateLimiter(), tokens);

    private Utilisateur compteExistant() {
        var user = new AccountCreationService(encoder, users).student("etudiant@usms.ac.ma", "correct", "Salma", "Alaoui",
                "D001", "MGSI");
        user.setStatus(AccountStatus.ACTIF);
        when(users.lockForLogin(user.getEmail(), AccountStatus.ACTIF)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void identifiantsValidesDonnentUnJeton() {
        Utilisateur user = compteExistant();
        when(tokens.generate(user.getEmail(), UserRole.STUDENT)).thenReturn("jwt");
        assertEquals("jwt", service.login(user.getEmail(), "correct").token());
    }

    @Test
    void mauvaisMotDePasseRefuse() {
        Utilisateur user = compteExistant();
        assertThrows(BadCredentialsException.class, () -> service.login(user.getEmail(), "incorrect"));
        verifyNoInteractions(tokens);
    }

    @Test
    void domaineExterneRefuse() {
        assertThrows(BadCredentialsException.class, () -> service.login("etudiant@example.org", "correct"));
        verifyNoInteractions(users, tokens);
    }

    @Test
    void loginNormaliseAdresse() {
        compteExistant();
        assertEquals(UserRole.STUDENT, service.login(" Etudiant@USMS.AC.MA ", "correct").role());
    }

    @Test
    void limitePartageeEntreLesVariantesDeLaMemeAdresse() {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(BadCredentialsException.class, () -> service.login("Etudiant@USMS.AC.MA", "faux"));
        }
        assertThrows(LoginLimitExceededException.class, () -> service.login(" etudiant@usms.ac.ma ", "faux"));
    }
}
