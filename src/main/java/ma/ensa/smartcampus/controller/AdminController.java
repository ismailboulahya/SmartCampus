package ma.ensa.smartcampus.controller;

import java.util.List;
import java.util.Map;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users() {
        return users.findAll().stream()
                .map(AdminController::resumer)
                .toList();
    }

    private static Map<String, Object> resumer(User utilisateur) {
        return Map.of(
                "id", utilisateur.getId(),
                "email", utilisateur.getEmail(),
                "firstName", utilisateur.getFirstName(),
                "lastName", utilisateur.getLastName(),
                "role", utilisateur.getRole().name(),
                "matricule", utilisateur.getMatricule() == null ? 0L : utilisateur.getMatricule(),
                "active", utilisateur.isActive());
    }
}
