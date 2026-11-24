package ma.ensa.smartcampus;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.Administrateur;
import ma.ensa.smartcampus.entity.Enseignant;
import ma.ensa.smartcampus.entity.Etudiant;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.service.AccountCreationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    private AccountCreationService creation;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void seed() {
        seedActive(creation.administrator("admin@usms.ac.ma", "Admin!2026", "Admin", "Test"));
        seedActive(creation.student("etudiant@usms.ac.ma", "Passw0rd!2026", "Lina", "Test", "TEST-CNE", "MGSI"));
        seedActive(creation.teacher("enseignant@usms.ac.ma", "Prof-test", "Ali", "Test", "TEST-MAT", "Informatique"));
    }

    private void seedActive(Utilisateur user) {
        if (!users.existsByEmail(user.getEmail())) {
            user.setStatus(AccountStatus.ACTIF);
            users.save(user);
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
                .andExpect(jsonPath("$[*].email", hasItem("admin@usms.ac.ma")));
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
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.status").value("ACTIF"))
                .andExpect(jsonPath("$.studentProfile.cne").value("TEST-CNE"))
                .andExpect(jsonPath("$.studentProfile.filiere").value("MGSI"))
                .andExpect(jsonPath("$.teacherProfile").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void enseignantConsulteSonProfil() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token("enseignant@usms.ac.ma", "Prof-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.teacherProfile.matricule").value("TEST-MAT"))
                .andExpect(jsonPath("$.teacherProfile.departement").value("Informatique"))
                .andExpect(jsonPath("$.studentProfile").doesNotExist());
    }

    @Test
    void deuxEnseignantsNePartagentPasUnMatricule() {
        var autre = creation.teacher("autre@usms.ac.ma", "test", "Test", "Deux", "TEST-MAT", "Informatique");
        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(autre));
    }

    @Test
    void filtresAdminSeCombinentEtNormalisentEmail() throws Exception {
        String bearer = "Bearer " + token("admin@usms.ac.ma", "Admin!2026");
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer)
                        .param("role", "TEACHER").param("status", "ACTIF")
                        .param("email", " Enseignant@USMS.AC.MA "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("enseignant@usms.ac.ma"))
                .andExpect(jsonPath("$[0].role").value("TEACHER"))
                .andExpect(jsonPath("$[0].status").value("ACTIF"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer)
                        .param("role", "STUDENT").param("email", "enseignant@usms.ac.ma"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer)
                        .param("status", "BLOQUE").param("email", "enseignant@usms.ac.ma"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer)
                        .param("role", "INCONNU"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void anciennesLignesGardentLeurSousTypeMemeSansDonneesUniversitaires() {
        for (UserRole role : UserRole.values()) {
            String email = "historique-" + role.name().toLowerCase(Locale.ROOT) + "@usms.ac.ma";
            jdbc.update("insert into users (email, password, role) values (?, ?, ?)", email, "hachage", role.name());
            Utilisateur user = users.findByEmailAndStatus(email, AccountStatus.EN_ATTENTE).orElseThrow();
            switch (role) {
                case ADMIN -> assertInstanceOf(Administrateur.class, user);
                case STUDENT -> assertInstanceOf(Etudiant.class, user);
                case TEACHER -> assertInstanceOf(Enseignant.class, user);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"EN_ATTENTE", "REFUSE", "BLOQUE"})
    void compteInactifNePeutNiSeConnecterNiUtiliserUnJetonExistant(AccountStatus etat) throws Exception {
        String email = etat.name().toLowerCase(Locale.ROOT) + "@usms.ac.ma";
        var user = creation.student(email, "test", "Test", "Statut", "CNE-" + etat, "MGSI");
        assertEquals(AccountStatus.EN_ATTENTE, user.getStatus());
        user.setStatus(AccountStatus.ACTIF);
        users.save(user);
        String jwt = jwtService.generate(user.getEmail(), UserRole.STUDENT);
        user.setStatus(etat);
        users.save(user);
        mockMvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "test"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
        mockMvc.perform(get("/api/db").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verrouillageTemporairePersistePuisExpire() throws Exception {
        String email = "essai-verrou@usms.ac.ma";
        var user = creation.student(email, "secret-correct", "Test", "Verrou", "CNE-VERROU", "GI");
        seedActive(user);
        var attempt = post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("email", email, "password", "incorrect")));
        for (int i = 0; i < 4; i++) mockMvc.perform(attempt).andExpect(status().isUnauthorized());
        mockMvc.perform(attempt).andExpect(status().isLocked());
        var correct = post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(Map.of("email", email, "password", "secret-correct")));
        mockMvc.perform(correct).andExpect(status().isLocked());
        var locked = users.findByEmailAndStatus(email, AccountStatus.ACTIF).orElseThrow();
        assertEquals(5, locked.getFailedLogins());
        locked.setLoginLockedUntil(java.time.Instant.now().minusSeconds(1));
        users.save(locked);
        mockMvc.perform(correct).andExpect(status().isOk());
        assertEquals(0, users.findByEmailAndStatus(email, AccountStatus.ACTIF).orElseThrow().getFailedLogins());
    }
}
