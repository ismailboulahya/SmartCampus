package ma.ensa.smartcampus.repository;

import java.util.Optional;
import ma.ensa.smartcampus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
