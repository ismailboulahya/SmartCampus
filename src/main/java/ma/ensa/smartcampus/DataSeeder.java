package ma.ensa.smartcampus;

import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.model.UserRole;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Comptes de démonstration réservés au profil "dev" : jamais exécuté en production.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("Ahmed", "El Amrani", "admin@usms.ac.ma", "Admin!2026", UserRole.ADMIN, 1001L);
        seed("Salma", "Bennis", "chef@usms.ac.ma", "Chef!2026", UserRole.CHEF_ATELIER, 1002L);
        seed("Youssef", "Kabbaj", "souschef@usms.ac.ma", "SousChef!2026", UserRole.SOUS_CHEF, 1003L);
    }

    private void seed(String firstName, String lastName, String email, String password, UserRole role,
            Long matricule) {
        if (!users.existsByEmail(email)) {
            users.save(new User(firstName, lastName, email, passwordEncoder.encode(password), role, matricule, 1));
        }
    }
}
