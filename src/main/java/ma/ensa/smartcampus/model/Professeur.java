package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("PROFESSEUR")
public class Professeur extends User {

    @Column(name = "matricule", length = 30)
    private String matricule;

    @Enumerated(EnumType.STRING)
    @Column(name = "departement", length = 30)
    private Departement departement;

    public Professeur() {
    }

    public Professeur(String nom, String prenom, String email, String motDePasseHash,
            String matricule, Departement departement) {
        super(nom, prenom, email, motDePasseHash);
        this.matricule = matricule;
        this.departement = departement;
    }

    @Override
    public UserRole getRole() {
        return UserRole.PROFESSEUR;
    }

    public String getMatricule() {
        return matricule;
    }

    public Departement getDepartement() {
        return departement;
    }
}
