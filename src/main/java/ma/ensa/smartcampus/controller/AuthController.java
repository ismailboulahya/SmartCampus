package ma.ensa.smartcampus.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.auth.LoginRateLimiter;
import ma.ensa.smartcampus.auth.LoginService;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    private final LoginService loginService;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final UserRepository users;

    public AuthController(LoginService loginService, JwtService jwtService, LoginRateLimiter rateLimiter,
            UserRepository users) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        if (rateLimiter.isBlocked(request.email())) {
            return ResponseEntity.status(429)
                    .body(Map.of("success", false, "message", "Trop de tentatives, réessayez plus tard."));
        }
        Optional<User> user = loginService.authenticate(request.email(), request.password());
        if (user.isEmpty()) {
            rateLimiter.recordFailure(request.email());
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Identifiants invalides."));
        }
        rateLimiter.reset(request.email());
        String token = jwtService.generate(user.get());
        return ResponseEntity.ok(Map.of("success", true, "token", token, "role", user.get().getRole()));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Optional<User> user = users.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Utilisateur inconnu."));
        }
        return ResponseEntity.ok(Map.of("success", true, "email", user.get().getEmail(), "role", user.get().getRole()));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true, "message", "Déconnexion réussie.");
    }
}
