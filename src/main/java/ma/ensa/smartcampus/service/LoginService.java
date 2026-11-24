package ma.ensa.smartcampus.service;

import java.time.Duration;
import java.time.Instant;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.auth.LoginRateLimiter;
import ma.ensa.smartcampus.dto.LoginResponse;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.exception.LoginLimitExceededException;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final JwtService jwtService;

    public LoginService(UserRepository users, PasswordEncoder passwordEncoder,
            LoginRateLimiter rateLimiter, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.jwtService = jwtService;
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
    public LoginResponse login(String email, String password) {
        String normalized = AccountService.normalizeEmail(email);
        if (normalized.endsWith("@usms.ac.ma")) {
            var account = users.lockForLogin(normalized, AccountStatus.ACTIF);
            if (account.isPresent()) {
                Utilisateur user = account.get();
                Instant now = Instant.now();
                if (user.getLoginLockedUntil() != null) {
                    if (user.getLoginLockedUntil().isAfter(now)) throw new LockedException("Compte verrouillé.");
                    user.setFailedLogins(0);
                    user.setLoginLockedUntil(null);
                }
                if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                    user.setFailedLogins(user.getFailedLogins() + 1);
                    if (user.getFailedLogins() >= 5) {
                        user.setLoginLockedUntil(now.plus(Duration.ofMinutes(15)));
                        throw new LockedException("Compte verrouillé.");
                    }
                    throw new BadCredentialsException("Identifiants invalides.");
                }
                user.setFailedLogins(0);
                rateLimiter.reinitialiser(normalized);
                UserRole role = AccountService.roleOf(user.getClass());
                return new LoginResponse(true, jwtService.generate(user.getEmail(), role), role);
            }
        }
        if (rateLimiter.estBloque(normalized)) throw new LoginLimitExceededException();
        rateLimiter.enregistrerEchec(normalized);
        throw new BadCredentialsException("Identifiants invalides.");
    }
}
