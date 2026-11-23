package ma.ensa.smartcampus.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import ma.ensa.smartcampus.entity.Inscription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inscription> findByEmailTokenHash(String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inscription> findBySetupTokenHash(String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscription i where i.userId = :id")
    Optional<Inscription> lockByUserId(long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscription i where i.user.email = :email")
    Optional<Inscription> lockByEmail(String email);

    @Query("select i.userId from Inscription i where i.nextMailAttemptAt <= :now order by i.nextMailAttemptAt, i.userId")
    List<Long> dueForMail(Instant now, Pageable page);
}
