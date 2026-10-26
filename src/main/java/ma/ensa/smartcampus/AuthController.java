package ma.ensa.smartcampus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ma.ensa.smartcampus.auth.LoginResult;
import ma.ensa.smartcampus.auth.LoginService;
import ma.ensa.smartcampus.security.AuthenticatedUser;
import ma.ensa.smartcampus.user.UserInfo;
import ma.ensa.smartcampus.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(
            @NotBlank(message = "L'email est obligatoire.")
            @Email(message = "Format d'email invalide.")
            @Size(max = 255)
            String email,
            @NotBlank(message = "Le mot de passe est obligatoire.")
            @Size(max = 128)
            String password) {}

    private final LoginService loginService;
    private final UserRepository userRepository;

    public AuthController(LoginService loginService, UserRepository userRepository) {
        this.loginService = loginService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginResult result = loginService.login(request.email(), request.password(), http.getRemoteAddr());
        return ResponseEntity.status(result.status().httpStatus()).body(result);
    }

    /** Profil de l'utilisateur connecté (nécessite un JWT). */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userRepository.findById(principal.id())
                .map(UserInfo::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
