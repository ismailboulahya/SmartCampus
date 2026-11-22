package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Column(name = "service_administratif", length = 100)
    private String serviceAdministratif;

    public Admin() {
    }

    public Admin(String nom, String prenom, String email, String motDePasseHash, String serviceAdministratif) {
        super(nom, prenom, email, motDePasseHash);
        this.serviceAdministratif = serviceAdministratif;
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    public String getServiceAdministratif() {
        return serviceAdministratif;
    }
}
