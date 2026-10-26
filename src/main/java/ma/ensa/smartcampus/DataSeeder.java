package ma.ensa.smartcampus;

import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.model.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("admin@usms.ac.ma", "Admin!2026", "ADMIN");
        seed("etudiant@usms.ac.ma", "Passw0rd!2026", "STUDENT");
    }

    private void seed(String email, String password, String role) {
        if (!users.existsByEmail(email)) {
            users.save(new User(null, email, passwordEncoder.encode(password), role));
        }
    }
}
