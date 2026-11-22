package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@DiscriminatorValue("ETUDIANT")
public class Etudiant extends User {

    @Column(name = "cne", unique = true, length = 20)
    private String cne;

    @Column(name = "cin", unique = true, length = 20)
    private String cin;

    @Enumerated(EnumType.STRING)
    @Column(name = "filiere", length = 20)
    private Filiere filiere;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", length = 20)
    private NiveauEducation niveau;

    public Etudiant() {
    }

    public Etudiant(String nom, String prenom, String email, String motDePasseHash,
            String cne, String cin, Filiere filiere, NiveauEducation niveau) {
        super(nom, prenom, email, motDePasseHash);
        this.cne = cne;
        this.cin = cin;
        this.filiere = filiere;
        this.niveau = niveau;
    }

    @Override
    public UserRole getRole() {
        return UserRole.ETUDIANT;
    }

    public String getCne() {
        return cne;
    }

    public String getCin() {
        return cin;
    }

    public Filiere getFiliere() {
        return filiere;
    }

    public NiveauEducation getNiveau() {
        return niveau;
    }
}
