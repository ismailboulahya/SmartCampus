package ma.ensa.smartcampus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.entity.UserRole;
import ma.ensa.smartcampus.entity.Utilisateur;
import ma.ensa.smartcampus.repository.UserRepository;
import ma.ensa.smartcampus.service.AccountCreationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDecisionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired AccountCreationService accounts;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;
    private Utilisateur admin;
    private Utilisateur candidate;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void prepare() {
        String suffix = UUID.randomUUID().toString();
        admin = accounts.administrator("admin-" + suffix + "@usms.ac.ma", "test-password", "Admin", "Test");
        admin.setStatus(AccountStatus.ACTIF);
        users.save(admin);
        candidate = accounts.student("s-" + suffix + "@usms.ac.ma", "test-password", "Test", "Campus", suffix, "MGSI");
        users.save(candidate);
        adminToken = "Bearer " + jwt.generate(admin.getEmail(), UserRole.ADMIN);
        studentToken = "Bearer " + jwt.generate(candidate.getEmail(), UserRole.STUDENT);
    }

    private void completeRegistration() {
        candidate.setEmailVerifiedAt(Instant.now());
        candidate.setMfaActivatedAt(Instant.now());
        users.save(candidate);
    }

    private ResultActions decide(long id, String from, String to, String token) throws Exception {
        var request = patch("/api/admin/users/{id}/statut", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("from", from, "to", to)));
        if (token != null) request.header("Authorization", token);
        return mvc.perform(request);
    }

    @Test
    void approvalBlockAndUnblockControlAccess() throws Exception {
        long id = candidate.getId();
        decide(id, "EN_ATTENTE", "ACTIF", adminToken).andExpect(status().isConflict());
        completeRegistration();
        decide(id, "EN_ATTENTE", "ACTIF", adminToken).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIF"));
        decide(id, "EN_ATTENTE", "REFUSE", adminToken).andExpect(status().isConflict());
        decide(id, "ACTIF", "BLOQUE", studentToken).andExpect(status().isForbidden());
        decide(id, "ACTIF", "BLOQUE", adminToken).andExpect(status().isOk());
        mvc.perform(get("/api/me").header("Authorization", studentToken)).andExpect(status().isUnauthorized());
        decide(id, "BLOQUE", "ACTIF", adminToken).andExpect(status().isOk());
        mvc.perform(get("/api/me").header("Authorization", studentToken)).andExpect(status().isOk());
    }

    @Test
    void onlyAdministratorCanRefuseAndCannotBlockSelf() throws Exception {
        completeRegistration();
        long id = candidate.getId();
        decide(id, "EN_ATTENTE", "REFUSE", null).andExpect(status().isUnauthorized());
        decide(id, "EN_ATTENTE", "REFUSE", adminToken).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUSE"));
        decide(id, "REFUSE", "ACTIF", adminToken).andExpect(status().isConflict());
        decide(admin.getId(), "ACTIF", "BLOQUE", adminToken).andExpect(status().isConflict());
        decide(Long.MAX_VALUE, "ACTIF", "BLOQUE", adminToken).andExpect(status().isNotFound());
    }
}
