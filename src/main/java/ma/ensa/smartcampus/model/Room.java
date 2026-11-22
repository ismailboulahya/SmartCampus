package ma.ensa.smartcampus.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Salle du campus : identifiant, nom unique, capacité strictement positive et équipements.
 */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Le nom de la salle est obligatoire.")
    @Size(min = 2, max = 100, message = "Le nom de la salle doit contenir entre 2 et 100 caractères.")
    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String nom;

    @NotNull(message = "La capacité de la salle est obligatoire.")
    @Positive(message = "La capacité de la salle doit être strictement positive.")
    @Column(name = "capacite", nullable = false)
    private Integer capacite;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_equipements", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "type_equipement", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<TypeEquipement> equipements = new LinkedHashSet<>();

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    protected Room() {
    }

    public Room(String nom, Integer capacite, Set<TypeEquipement> equipements) {
        this.nom = nom;
        this.capacite = capacite;
        setEquipements(equipements);
    }

    @PrePersist
    void renseignerLaDateDeCreation() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        dateModification = maintenant;
    }

    @PreUpdate
    void renseignerLaDateDeModification() {
        dateModification = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }

    public Set<TypeEquipement> getEquipements() {
        return equipements;
    }

    public void setEquipements(Set<TypeEquipement> equipements) {
        this.equipements = new LinkedHashSet<>();
        if (equipements != null) {
            equipements.stream().filter(Objects::nonNull).forEach(this.equipements::add);
        }
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }
}
