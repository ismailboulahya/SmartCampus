package ma.ensa.smartcampus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.ensa.smartcampus.user.Role;
import ma.ensa.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Démarre l'application complète sur H2 : Flyway, sécurité, JWT, contrôleurs. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String STUDENT = "etudiant@usms.ac.ma";
    private static final String ADMIN = "admin@usms.ac.ma";
    private static final String PASSWORD = "Passw0rd!2026";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @BeforeEach
    void resetDatabase() {
        jdbc.update("DELETE FROM login_audit");
        jdbc.update("DELETE FROM users");
        String hash = encoder.encode(PASSWORD);
        users.create(STUDENT, hash, "Etudiant", "Test", Role.STUDENT);
        users.create(ADMIN, hash, "Admin", "Test", Role.ADMIN);
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = json.writeValueAsString(new AuthController.LoginRequest(email, password));
        return mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String tokenFor(String email) throws Exception {
        String response = login(email, PASSWORD).andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("token").asText();
    }

    @Test
    void migrationsFlywayAppliquees() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE AND version IS NOT NULL",
                Integer.class);
        assertThat(applied).isEqualTo(2);
    }

    @Test
    void connexionReussieRenvoieUnTokenUtilisable() throws Exception {
        login(STUDENT, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());

        mvc.perform(get("/api/me").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(STUDENT));
    }

    @Test
    void mauvaisMotDePasseRetourne401EtEstJournalise() throws Exception {
        login(STUDENT, "faux")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.token").doesNotExist());

        String reason = jdbc.queryForObject(
                "SELECT reason FROM login_audit WHERE email = ?", String.class, STUDENT);
        assertThat(reason).isEqualTo("BAD_PASSWORD");
    }

    @Test
    void verrouillageApresCinqEchecs() throws Exception {
        for (int i = 0; i < 4; i++) {
            login(STUDENT, "faux").andExpect(status().isUnauthorized());
        }
        login(STUDENT, "faux").andExpect(status().is(423));
        login(STUDENT, PASSWORD).andExpect(status().is(423));
    }

    @Test
    void routeProtegeeSansTokenRetourne401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false));

        mvc.perform(get("/api/me").header("Authorization", "Bearer token.invalide"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routeAdminReserveeAuRoleAdmin() throws Exception {
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + tokenFor(STUDENT)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + tokenFor(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    void requeteInvalideRetourne400() throws Exception {
        login("pas-un-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void endpointsPublicsAccessibles() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/db")).andExpect(status().isOk());
    }
}
