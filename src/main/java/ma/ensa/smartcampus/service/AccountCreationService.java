package ma.ensa.smartcampus.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.Administrateur;
import ma.ensa.smartcampus.entity.Enseignant;
import ma.ensa.smartcampus.entity.Etudiant;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountCreationService {

    private final PasswordEncoder encoder;
    private final UserRepository users;

    public Etudiant student(String email, String password, String firstName, String lastName,
            String cne, String filiere) {
        var user = new Etudiant();
        user.setCne(required(cne));
        user.setFiliere(required(filiere));
        initialize(user, email, password, firstName, lastName);
        return user;
    }

    public Enseignant teacher(String email, String password, String firstName, String lastName,
            String matricule, String departement) {
        var user = new Enseignant();
        user.setMatricule(required(matricule));
        user.setDepartement(required(departement));
        initialize(user, email, password, firstName, lastName);
        return user;
    }

    public Administrateur administrator(String email, String password, String firstName, String lastName) {
        var user = new Administrateur();
        initialize(user, email, password, firstName, lastName);
        return user;
    }

    private void initialize(Utilisateur user, String email, String password, String firstName, String lastName) {
        user.setEmail(AccountService.normalizeEmail(required(email)));
        user.setFirstName(required(firstName));
        user.setLastName(required(lastName));
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        user.setPasswordHash(encoder.encode(password));
        user.setCreatedAt(Instant.now());
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Les informations du compte doivent être renseignées.");
        }
        return value.strip();
    }

    public void createDemoAccounts(String password) {
        seedActive(administrator("admin@usms.ac.ma", password, "Admin", "Démo"));
        seedActive(student("etudiant@usms.ac.ma", password, "Étudiant", "Démo", "DEMO-CNE", "MGSI"));
        seedActive(teacher("enseignant@usms.ac.ma", password, "Enseignant", "Démo", "DEMO-MAT", "Informatique"));
    }

    private void seedActive(Utilisateur user) {
        if (users.existsByEmail(user.getEmail())) {
            return;
        }
        user.setStatus(AccountStatus.ACTIF);
        try {
            users.save(user);
        } catch (DataIntegrityViolationException ex) {
            if (!users.existsByEmail(user.getEmail())) {
                throw ex;
            }
        }
    }
}
