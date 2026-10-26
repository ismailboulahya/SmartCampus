package ma.ensa.smartcampus;

import java.util.Map;
import ma.ensa.smartcampus.auth.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(String email, String password) {}

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        boolean ok = loginService.authenticate(request.email(), request.password());
        if (ok) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Connexion réussie."));
        }
        return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Identifiants invalides."));
    }
}
