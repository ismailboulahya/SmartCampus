package ma.ensa.smartcampus;

import ma.ensa.smartcampus.model.Admin;
import ma.ensa.smartcampus.model.Departement;
import ma.ensa.smartcampus.model.Etudiant;
import ma.ensa.smartcampus.model.Filiere;
import ma.ensa.smartcampus.model.NiveauEducation;
import ma.ensa.smartcampus.model.Professeur;
import ma.ensa.smartcampus.model.SuperAdmin;
import ma.ensa.smartcampus.model.User;
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
        seed(new SuperAdmin("Nadia", "Alaoui", "superadmin@usms.ac.ma", encoder("SuperAdmin!2026"), "TOTAL"));
        seed(new Admin("Ahmed", "El Amrani", "admin@usms.ac.ma", encoder("Admin!2026"), "Scolarité"));
        seed(new Professeur("Karim", "Idrissi", "prof@usms.ac.ma", encoder("Prof!2026"),
                "P-1001", Departement.GENIE_ELECTRIQUE));
        seed(new Etudiant("Youssef", "Kabbaj", "etudiant@usms.ac.ma", encoder("Etudiant!2026"),
                "E123456", "AB123456", Filiere.GI, NiveauEducation.CYCLE1));
    }

    private String encoder(String motDePasse) {
        return passwordEncoder.encode(motDePasse);
    }

    private void seed(User utilisateur) {
        if (!users.existsByEmail(utilisateur.getEmail())) {
            users.save(utilisateur);
        }
    }
}
