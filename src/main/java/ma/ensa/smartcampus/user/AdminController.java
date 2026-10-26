package ma.ensa.smartcampus.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints réservés au rôle ADMIN (règle dans SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserInfo> listUsers() {
        return users.findAll().stream().map(UserInfo::from).toList();
    }
}
