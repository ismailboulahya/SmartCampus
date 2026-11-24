package ma.ensa.smartcampus.repository;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

public interface UserRepository extends JpaRepository<Utilisateur, Long>, JpaSpecificationExecutor<Utilisateur> {

    Optional<Utilisateur> findByEmailAndStatus(String email, AccountStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.email = :email and u.status = :status")
    Optional<Utilisateur> lockForLogin(String email, AccountStatus status);

    @Query("select type(u) from Utilisateur u where u.email = :email and u.status = :status")
    Optional<Class<? extends Utilisateur>> findTypeByEmailAndStatus(String email, AccountStatus status);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.id = :id or u.email = :actor order by u.id")
    List<Utilisateur> lockForDecision(long id, String actor);
}
