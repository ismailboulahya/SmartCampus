package ma.ensa.smartcampus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "user_fname", nullable = false, length = 100)
    private String firstName;

    @Column(name = "user_lname", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 30)
    private UserRole role;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String password;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "user_matricule", unique = true)
    private Long matricule;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    @Column(name = "is_locked", nullable = false)
    private boolean locked;

    public User() {
    }

    public User(String firstName, String lastName, String email, String password, UserRole role,
            Long matricule, Integer departmentId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.matricule = matricule;
        this.departmentId = departmentId;
    }

    @PrePersist
    void renseignerLaDateDeCreation() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /** Le compte est bloqué seulement tant que la date de fin de verrouillage n'est pas dépassée. */
    public boolean estVerrouille(LocalDateTime maintenant) {
        return locked && lockoutEnd != null && lockoutEnd.isAfter(maintenant);
    }

    public void enregistrerEchecDeConnexion(int maximum, Duration duree, LocalDateTime maintenant) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maximum) {
            locked = true;
            lockoutEnd = maintenant.plus(duree);
        }
    }

    public void reinitialiserLesTentatives() {
        failedLoginAttempts = 0;
        locked = false;
        lockoutEnd = null;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getMatricule() {
        return matricule;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public LocalDateTime getLockoutEnd() {
        return lockoutEnd;
    }

    public boolean isLocked() {
        return locked;
    }
}
