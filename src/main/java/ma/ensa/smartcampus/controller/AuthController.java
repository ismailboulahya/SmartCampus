package ma.ensa.smartcampus.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Optional;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.auth.LoginRateLimiter;
import ma.ensa.smartcampus.auth.LoginService;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.repository.UserRepository;
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
        if (rateLimiter.estBloque(request.email())) {
            return ResponseEntity.status(429)
                    .body(Map.of("success", false, "message", "Trop de tentatives, réessayez plus tard."));
        }

        LoginService.Resultat resultat = loginService.authentifier(request.email(), request.password());
        return switch (resultat.statut()) {
            case COMPTE_VERROUILLE -> ResponseEntity.status(423)
                    .body(Map.of("success", false, "message", "Compte verrouillé, réessayez dans quelques minutes."));
            case IDENTIFIANTS_INVALIDES -> {
                rateLimiter.enregistrerEchec(request.email());
                yield ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Identifiants invalides."));
            }
            case SUCCES -> {
                rateLimiter.reinitialiser(request.email());
                User utilisateur = resultat.utilisateur();
                yield ResponseEntity.ok(Map.of(
                        "success", true,
                        "token", jwtService.generate(utilisateur),
                        "role", utilisateur.getRole().name(),
                        "nom", utilisateur.getNom(),
                        "prenom", utilisateur.getPrenom()));
            }
        };
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Optional<User> user = users.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Utilisateur inconnu."));
        }
        User utilisateur = user.get();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "email", utilisateur.getEmail(),
                "nom", utilisateur.getNom(),
                "prenom", utilisateur.getPrenom(),
                "role", utilisateur.getRole().name()));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true, "message", "Déconnexion réussie.");
    }
}
