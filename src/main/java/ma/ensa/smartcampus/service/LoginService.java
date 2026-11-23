package ma.ensa.smartcampus.service;

import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.auth.LoginRateLimiter;
import ma.ensa.smartcampus.dto.LoginResponse;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.exception.LoginLimitExceededException;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public LoginResponse login(String email, String password) {
        String normalized = AccountService.normalizeEmail(email);
        if (rateLimiter.isBlocked(normalized)) {
            throw new LoginLimitExceededException();
        }
        if (normalized.endsWith("@usms.ac.ma")) {
            var account = users.findByEmailAndStatus(normalized, AccountStatus.ACTIF)
                    .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()));
            if (account.isPresent()) {
                Utilisateur user = account.get();
                rateLimiter.reset(normalized);
                UserRole role = AccountService.roleOf(user.getClass());
                return new LoginResponse(true, jwtService.generate(user.getEmail(), role), role);
            }
        }
        rateLimiter.recordFailure(normalized);
        throw new BadCredentialsException("Identifiants invalides.");
    }
}
