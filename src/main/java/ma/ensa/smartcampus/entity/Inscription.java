package ma.ensa.smartcampus.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Inscription {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Utilisateur user;

    private String emailTokenHash;
    private Instant emailExpiresAt;
    private String encryptedMailToken;
    private Instant nextMailAttemptAt;
    private Instant lastLinkIssuedAt;
    private String setupTokenHash;
    private Instant setupExpiresAt;
    private String encryptedTotpSecret;
    private int failedProofs;
    private Instant blockedUntil;
}
