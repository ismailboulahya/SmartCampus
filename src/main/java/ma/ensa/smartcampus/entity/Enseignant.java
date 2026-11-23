package ma.ensa.smartcampus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("TEACHER")
@Getter
@Setter
@NoArgsConstructor
public class Enseignant extends Utilisateur {

    @Column(unique = true)
    private String matricule;

    private String departement;
}
