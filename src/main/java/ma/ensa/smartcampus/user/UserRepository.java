package ma.ensa.smartcampus.user;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Accès à la table users avec JdbcTemplate. */
@Repository
public class UserRepository {

    private static final String COLUMNS =
            "id, email, password_hash, first_name, last_name, role, enabled, "
            + "failed_attempts, locked_until, last_login_at";

    private static final RowMapper<User> MAPPER = UserRepository::mapRow;

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** L'email doit déjà être normalisé (trim + minuscules). */
    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE email = ?", MAPPER, email)
                .stream().findFirst();
    }

    public Optional<User> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM users WHERE id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public List<User> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM users ORDER BY id", MAPPER);
    }

    /** Crée un utilisateur et retourne son id. passwordHash doit déjà être haché. */
    public long create(String email, String passwordHash, String firstName, String lastName, Role role) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users (email, password_hash, first_name, last_name, role) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    new String[] {"id"});
            ps.setString(1, email);
            ps.setString(2, passwordHash);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, role.name());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /**
     * Incrémente le compteur d'échecs. Quand il atteint maxAttempts, le compte
     * est verrouillé jusqu'à lockUntil et le compteur repart à zéro.
     */
    public void recordFailedAttempt(long id, int maxAttempts, OffsetDateTime lockUntil, OffsetDateTime now) {
        jdbc.update("""
                UPDATE users SET
                    locked_until    = CASE WHEN failed_attempts + 1 >= ? THEN CAST(? AS TIMESTAMP WITH TIME ZONE) ELSE locked_until END,
                    failed_attempts = CASE WHEN failed_attempts + 1 >= ? THEN 0 ELSE failed_attempts + 1 END,
                    updated_at      = ?
                WHERE id = ?
                """, maxAttempts, lockUntil, maxAttempts, now, id);
    }

    public void recordSuccessfulLogin(long id, OffsetDateTime now) {
        jdbc.update("""
                UPDATE users SET
                    failed_attempts = 0,
                    locked_until    = NULL,
                    last_login_at   = ?,
                    updated_at      = ?
                WHERE id = ?
                """, now, now, id);
    }

    private static User mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("enabled"),
                rs.getInt("failed_attempts"),
                rs.getObject("locked_until", OffsetDateTime.class),
                rs.getObject("last_login_at", OffsetDateTime.class));
    }
}
