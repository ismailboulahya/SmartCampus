package ma.ensa.smartcampus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import ma.ensa.smartcampus.model.Admin;
import ma.ensa.smartcampus.model.Etudiant;
import ma.ensa.smartcampus.model.Filiere;
import ma.ensa.smartcampus.model.NiveauEducation;
import ma.ensa.smartcampus.model.User;
import ma.ensa.smartcampus.repository.UserRepository;
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
        if (!users.existsByEmail("admin@usms.ac.ma")) {
            users.save(new Admin("El Amrani", "Ahmed", "admin@usms.ac.ma",
                    passwordEncoder.encode("Admin!2026"), "Scolarité"));
        }
        if (!users.existsByEmail("etudiant@usms.ac.ma")) {
            users.save(new Etudiant("Kabbaj", "Youssef", "etudiant@usms.ac.ma",
                    passwordEncoder.encode("Etudiant!2026"), "E123456", "AB123456",
                    Filiere.GI, NiveauEducation.CYCLE1));
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
                .andExpect(jsonPath("$.nom").value("El Amrani"))
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
    void loginSurCompteVerrouilleRetourne423() throws Exception {
        String email = "verrouille@usms.ac.ma";
        if (!users.existsByEmail(email)) {
            User compte = new Etudiant("Verrouille", "Compte", email,
                    passwordEncoder.encode("Verrouille!2026"), "E999999", "AB999999",
                    Filiere.GI, NiveauEducation.CYCLE1);
            for (int tentative = 1; tentative <= 5; tentative++) {
                compte.enregistrerEchecDeConnexion(5, Duration.ofMinutes(15), LocalDateTime.now());
            }
            users.save(compte);
        }
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Verrouille!2026\"}"))
                .andExpect(status().isLocked());
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
                        .header("Authorization", "Bearer " + token("etudiant@usms.ac.ma", "Etudiant!2026")))
                .andExpect(status().isForbidden());
    }

    @Test
    void meRetourneLUtilisateurCourant() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token("etudiant@usms.ac.ma", "Etudiant!2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("etudiant@usms.ac.ma"))
                .andExpect(jsonPath("$.prenom").value("Youssef"))
                .andExpect(jsonPath("$.role").value("ETUDIANT"));
    }
}
