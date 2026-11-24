package ma.ensa.smartcampus.config;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** The rooms branch shipped different V1/V2 scripts. Keep their recorded checksums valid. */
@Component
public class UserSchemaHistory implements FlywayConfigurationCustomizer {
    @Override
    public void customize(FluentConfiguration config) {
        var jdbc = new JdbcTemplate(config.getDataSource());
        boolean oldUsers = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE LOWER(table_schema) = LOWER(CURRENT_SCHEMA) AND LOWER(table_name) = 'users'
                  AND LOWER(column_name) = 'type_utilisateur'
                """, Integer.class) > 0;
        boolean history = tableExists(jdbc, "flyway_schema_history");
        boolean oldHistory = history && jdbc.queryForObject("""
                SELECT COUNT(*) FROM "flyway_schema_history" WHERE "success" = TRUE
                AND "script" IN ('V1__create_users.sql', 'V2__create_rooms.sql', 'V2_1__align_user_accounts.sql')
                """, Integer.class) > 0;
        if (oldUsers || oldHistory) {
            config.locations("classpath:db/legacy-rooms", "classpath:db/migration/shared");
            if (!history && tableExists(jdbc, "rooms") && tableExists(jdbc, "room_equipements")) {
                config.baselineVersion("2");
            }
        }
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE LOWER(table_schema) = LOWER(CURRENT_SCHEMA) AND LOWER(table_name) = ?
                """, Integer.class, table) > 0;
    }
}
