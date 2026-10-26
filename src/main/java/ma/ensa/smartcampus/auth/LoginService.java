package ma.ensa.smartcampus.auth;

import java.util.Optional;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private static final String DOMAINE_INSTITUTIONNEL = "@usms.ac.ma";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> authenticate(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        String normalise = email.trim().toLowerCase();
        if (!normalise.endsWith(DOMAINE_INSTITUTIONNEL)) {
            return Optional.empty();
        }
        return users.findByEmail(normalise)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()));
    }
}
