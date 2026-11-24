package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import ma.ensa.smartcampus.config.UserSchemaHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
            assertEquals(4, flyway.migrate().migrationsExecuted);
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void reunitLesDeuxHistoriquesSansPerdreLesComptesNiLesSalles(boolean avecHistorique) throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            if (avecHistorique) {
                Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/legacy-rooms")
                        .target("2").load().migrate();
            } else {
                sql.execute("RUNSCRIPT FROM 'classpath:db/legacy-rooms/V1__create_users.sql'");
                sql.execute("RUNSCRIPT FROM 'classpath:db/legacy-rooms/V2__create_rooms.sql'");
            }
            sql.executeUpdate("""
                    INSERT INTO users(type_utilisateur, nom, prenom, email, mdp_hash, cne, filiere, cin)
                    VALUES ('ETUDIANT','Nom','Prénom','Test@USMS.AC.MA','hash-origine','CNE1','GI','CIN1')
                    """);
            sql.executeUpdate("INSERT INTO rooms(nom, capacite) VALUES ('Amphi', 35)");
            sql.executeUpdate("""
                    INSERT INTO users(type_utilisateur, nom, prenom, email, mdp_hash, matricule)
                    VALUES ('PROFESSEUR','Nom','Prénom','prof@usms.ac.ma','hash-prof','M1')
                    """);
            sql.executeUpdate("INSERT INTO room_equipements VALUES (1, 'TABLEAU')");
            var config = Flyway.configure().dataSource(url, "sa", "").baselineOnMigrate(true);
            new UserSchemaHistory().customize(config);
            config.load().migrate();
            var restart = Flyway.configure().dataSource(url, "sa", "");
            new UserSchemaHistory().customize(restart);
            assertEquals(0, restart.load().migrate().migrationsExecuted);
            try (var row = sql.executeQuery("SELECT id, role, status, password, email, cin FROM users WHERE id = 1")) {
                assertTrue(row.next());
                assertEquals(1L, row.getLong("id"));
                assertEquals("STUDENT", row.getString("role"));
                assertEquals("EN_ATTENTE", row.getString("status"));
                assertEquals("hash-origine", row.getString("password"));
                assertEquals("test@usms.ac.ma", row.getString("email"));
                assertEquals("CIN1", row.getString("cin"));
            }
            try (var row = sql.executeQuery("SELECT role, status, matricule, departement FROM users WHERE id = 2")) {
                assertTrue(row.next());
                assertEquals("TEACHER", row.getString("role"));
                assertEquals("EN_ATTENTE", row.getString("status"));
                assertEquals("M1", row.getString("matricule"));
                assertNull(row.getString("departement"));
            }
            try (var row = sql.executeQuery("SELECT nom, type_equipement FROM rooms JOIN room_equipements ON id=room_id")) {
                assertTrue(row.next());
                assertEquals("Amphi", row.getString(1));
                assertEquals("TABLEAU", row.getString(2));
            }
        }
    }
}
