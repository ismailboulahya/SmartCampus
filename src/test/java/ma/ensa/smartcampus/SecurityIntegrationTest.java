package ma.ensa.smartcampus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void seed() {
        seedUser("admin@usms.ac.ma", "Admin!2026", "ADMIN");
        seedUser("etudiant@usms.ac.ma", "Passw0rd!2026", "STUDENT");
    }

    private void seedUser(String email, String password, String role) {
        if (!users.existsByEmail(email)) {
            users.save(new User(null, email, passwordEncoder.encode(password), role));
        }
    }

    private String token(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void loginAvecIdentifiantsValidesRetourneUnToken() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@usms.ac.ma\",\"password\":\"Admin!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginAvecMotDePasseInvalideRetourne401() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@usms.ac.ma\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSansEmailRetourne400() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"faux\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void routeAdminSansTokenRetourne401() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void routeAdminAvecTokenAdminRetourneLaListe() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token("admin@usms.ac.ma", "Admin!2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@usms.ac.ma"));
    }

    @Test
    void routeAdminAvecTokenEtudiantRetourne403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token("etudiant@usms.ac.ma", "Passw0rd!2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meRetourneLUtilisateurCourant() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token("etudiant@usms.ac.ma", "Passw0rd!2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("etudiant@usms.ac.ma"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
