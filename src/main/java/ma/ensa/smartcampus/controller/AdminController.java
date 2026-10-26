package ma.ensa.smartcampus.controller;

import java.util.List;
import java.util.Map;
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
                .map(user -> Map.<String, Object>of("id", user.getId(), "email", user.getEmail(), "role", user.getRole()))
                .toList();
    }
}
