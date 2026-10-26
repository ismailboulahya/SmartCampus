package ma.ensa.smartcampus.auth;

import java.time.OffsetDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Écriture dans la table login_audit. */
@Repository
public class LoginAuditRepository {

    private final JdbcTemplate jdbc;

    public LoginAuditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String email, Long userId, LoginStatus status, String ipAddress, OffsetDateTime at) {
        jdbc.update("""
                INSERT INTO login_audit (email, user_id, success, reason, ip_address, attempted_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                truncate(email, 255), userId, status == LoginStatus.SUCCESS, status.name(),
                truncate(ipAddress, 45), at);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
