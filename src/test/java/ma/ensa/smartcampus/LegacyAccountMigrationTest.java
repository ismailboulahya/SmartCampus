package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LegacyAccountMigrationTest {

    @Test
    void migrerUnSchemaSansHistoriqueConserveLeCompteEnAttente() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
                var sql = connection.createStatement()) {
            sql.execute("RUNSCRIPT FROM 'classpath:db/migration/V1__initial_users.sql'");
            sql.executeUpdate("""
                    INSERT INTO users (email, password, role)
                    VALUES ('Ancien@USMS.AC.MA', 'hachage-existant', 'ADMIN')
                    """);
            var flyway = Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true).load();
            assertEquals(2, flyway.migrate().migrationsExecuted);
            assertEquals(0, flyway.migrate().migrationsExecuted);

            try (var row = sql.executeQuery("SELECT * FROM users")) {
                assertTrue(row.next());
                assertEquals(1L, row.getLong("id"));
                assertEquals("ancien@usms.ac.ma", row.getString("email"));
                assertEquals("hachage-existant", row.getString("password"));
                assertEquals("ADMIN", row.getString("role"));
                assertEquals("EN_ATTENTE", row.getString("status"));
                assertNull(row.getObject("created_at"));
                assertNull(row.getObject("matricule"));
                assertFalse(row.next());
            }
        }
    }
}
