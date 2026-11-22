package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Utilisateur de la plateforme, conformément au diagramme de classes.
 * Les spécialisations (Étudiant, Professeur, Admin, SuperAdmin) partagent la même table.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type_utilisateur", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "mdp_hash", nullable = false, length = 100)
    private String motDePasseHash;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "photo_profil_url", length = 500)
    private String photoProfilUrl;

    @Column(name = "photo_couverture_url", length = 500)
    private String photoCouvertureUrl;

    @Column(name = "nbr_connexion_echouee", nullable = false)
    private int nbrConnexionEchouee;

    @Column(name = "est_verrouille", nullable = false)
    private boolean verrouille;

    @Column(name = "fin_verrouillage")
    private LocalDateTime finVerrouillage;

    protected User() {
    }

    protected User(String nom, String prenom, String email, String motDePasseHash) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasseHash = motDePasseHash;
    }

    @PrePersist
    void renseignerLaDateDeCreation() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }

    /** Rôle applicatif déduit du type d'utilisateur, utilisé pour les autorisations. */
    public abstract UserRole getRole();

    /** Le compte reste bloqué seulement tant que la date de fin de verrouillage n'est pas dépassée. */
    public boolean estVerrouille(LocalDateTime maintenant) {
        return verrouille && finVerrouillage != null && finVerrouillage.isAfter(maintenant);
    }

    public void enregistrerEchecDeConnexion(int maximum, Duration duree, LocalDateTime maintenant) {
        nbrConnexionEchouee++;
        if (nbrConnexionEchouee >= maximum) {
            verrouille = true;
            finVerrouillage = maintenant.plus(duree);
        }
    }

    public void reinitialiserLesTentatives() {
        nbrConnexionEchouee = 0;
        verrouille = false;
        finVerrouillage = null;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public String getMotDePasseHash() {
        return motDePasseHash;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public String getPhotoProfilUrl() {
        return photoProfilUrl;
    }

    public void setPhotoProfilUrl(String photoProfilUrl) {
        this.photoProfilUrl = photoProfilUrl;
    }

    public String getPhotoCouvertureUrl() {
        return photoCouvertureUrl;
    }

    public void setPhotoCouvertureUrl(String photoCouvertureUrl) {
        this.photoCouvertureUrl = photoCouvertureUrl;
    }

    public int getNbrConnexionEchouee() {
        return nbrConnexionEchouee;
    }

    public boolean isVerrouille() {
        return verrouille;
    }

    public LocalDateTime getFinVerrouillage() {
        return finVerrouillage;
    }
}
