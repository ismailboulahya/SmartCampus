package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SUPERADMIN")
public class SuperAdmin extends User {

    @Column(name = "niveau_acces", length = 50)
    private String niveauAcces;

    public SuperAdmin() {
    }

    public SuperAdmin(String nom, String prenom, String email, String motDePasseHash, String niveauAcces) {
        super(nom, prenom, email, motDePasseHash);
        this.niveauAcces = niveauAcces;
    }

    @Override
    public UserRole getRole() {
        return UserRole.SUPERADMIN;
    }

    public String getNiveauAcces() {
        return niveauAcces;
    }
}
