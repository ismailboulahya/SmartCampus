package ma.ensa.smartcampus;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.crypto.spec.SecretKeySpec;
import ma.ensa.smartcampus.auth.JwtService;
import ma.ensa.smartcampus.entity.*;
import ma.ensa.smartcampus.repository.*;
import ma.ensa.smartcampus.service.*;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");
    private static final String PASSWORD = "MonPass-Test42";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired InscriptionRepository inscriptions;
    @Autowired UserRepository users;
    @Autowired RegistrationMailService mail;
    @Autowired AccountCreationService accounts;
    @Autowired JwtService jwt;
    @MockBean JavaMailSender smtp;
    @MockBean Clock clock;
    private String email;

    @BeforeEach
    void prepare() {
        email = UUID.randomUUID() + "@usms.ac.ma";
        when(clock.instant()).thenReturn(NOW);
    }

    private Map<String, Object> request(String role) {
        return Map.of("role", role, "email", email, "password", PASSWORD, "firstName", "Lina", "lastName", "Test",
                role.equals("TEACHER") ? "teacherProfile" : "studentProfile",
                role.equals("TEACHER") ? Map.of("matricule", email, "departement", "Informatique")
                        : Map.of("cne", email, "filiere", "MGSI"));
    }

    private ResultActions send(String route, Object body) throws Exception {
        return mvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)));
    }

    private Utilisateur candidate() {
        return users.findByEmailAndStatus(email, AccountStatus.EN_ATTENTE).orElseThrow();
    }

    private String sentLink() {
        mail.deliver(candidate().getId());
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp, atLeastOnce()).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertArrayEquals(new String[]{email}, message.getTo());
        var match = Pattern.compile("/inscription\\.html#([A-Za-z0-9_-]{43})").matcher(message.getText());
        assertTrue(match.find());
        return match.group(1);
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return send("/api/inscriptions/email/confirmer", Map.of("token", token, "password", password));
    }

    private JsonNode setup(String token) throws Exception {
        var response = confirm(token, PASSWORD).andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsByteArray());
    }

    private String code(JsonNode setup, Instant instant) throws Exception {
        var generator = new TimeBasedOneTimePasswordGenerator();
        var key = new SecretKeySpec(new Base32().decode(setup.get("manualKey").asText()), generator.getAlgorithm());
        return generator.generateOneTimePasswordString(key, instant, Locale.ROOT);
    }

    private ResultActions activate(JsonNode setup, String code) throws Exception {
        return send("/api/inscriptions/mfa/activer", Map.of("setupToken", setup.get("setupToken").asText(), "code", code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "TEACHER"})
    void inscriptionCompleteResteEnAttenteEtDevientVisiblePourAdministration(String role) throws Exception {
        send("/api/inscriptions", request(role)).andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist());
        Utilisateur user = candidate();
        assertNull(user.getEmailVerifiedAt());
        assertNotEquals(PASSWORD, user.getPasswordHash());
        assertEquals(role.equals("TEACHER") ? Enseignant.class : Etudiant.class, user.getClass());
        var admin = accounts.administrator("gestion-" + email, PASSWORD, "Admin", "Test");
        admin.setStatus(AccountStatus.ACTIF);
        users.save(admin);
        String auth = "Bearer " + jwt.generate(admin.getEmail(), UserRole.ADMIN);

        String link = sentLink();
        confirm(link, "incorrect").andExpect(status().isUnauthorized());
        JsonNode setup = setup(link);
        assertNotNull(candidate().getEmailVerifiedAt());
        assertNull(candidate().getMfaActivatedAt());
        confirm(link, PASSWORD).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/inscriptions").header("Authorization", auth))
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.not(hasItem(email))));
        String setupToken = setup.get("setupToken").asText();
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + setupToken)).andExpect(status().isUnauthorized());
        String otp = code(setup, NOW);
        activate(setup, otp).andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist());
        activate(setup, otp).andExpect(status().isUnauthorized());
        assertNotNull(candidate().getMfaActivatedAt());
        var entry = inscriptions.findById(user.getId()).orElseThrow();
        assertNull(entry.getSetupTokenHash());
        assertNull(entry.getEncryptedMailToken());
        assertNotEquals(entry.getEncryptedTotpSecret(), setup.get("manualKey").asText());
        send("/api/login", Map.of("email", email, "password", PASSWORD)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/inscriptions").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].email", hasItem(email)));
        mvc.perform(get("/api/admin/inscriptions")).andExpect(status().isUnauthorized());
    }

    @Test
    void doublonEtConflitDeCneConserventLeDossierInitial() throws Exception {
        var first = request("STUDENT");
        String answer = send("/api/inscriptions", first).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        var entry = inscriptions.findById(candidate().getId()).orElseThrow();
        var duplicate = new java.util.HashMap<>(first);
        duplicate.put("email", " " + email.toUpperCase(Locale.ROOT) + " ");
        duplicate.put("password", "Different-234");
        send("/api/inscriptions", duplicate).andExpect(status().isAccepted()).andExpect(content().string(answer));
        duplicate.put("email", "autre-" + email);
        send("/api/inscriptions", duplicate).andExpect(status().isAccepted()).andExpect(content().string(answer));
        assertFalse(users.existsByEmail("autre-" + email));
        assertEquals(entry.getEmailTokenHash(), inscriptions.findById(candidate().getId()).orElseThrow().getEmailTokenHash());
        setup(sentLink());
    }

    @Test
    void refuseAdministrateurDomaineExterneEtProfilIncoherent() throws Exception {
        send("/api/inscriptions", request("ADMIN")).andExpect(status().isBadRequest());
        var invalid = new java.util.HashMap<>(request("STUDENT"));
        invalid.put("teacherProfile", Map.of("matricule", "mat", "departement", "dept"));
        send("/api/inscriptions", invalid).andExpect(status().isBadRequest());
        invalid.remove("teacherProfile");
        invalid.put("email", "inconnu@example.com");
        send("/api/inscriptions", invalid).andExpect(status().isBadRequest());
        assertFalse(users.existsByEmail(email));
    }

    @Test
    void expirationRenvoiEtRepriseRemplacentLesAnciennesPreuves() throws Exception {
        send("/api/inscriptions", request("STUDENT")).andExpect(status().isAccepted());
        String old = sentLink();
        when(clock.instant()).thenReturn(NOW.plusSeconds(1800));
        confirm(old, PASSWORD).andExpect(status().isUnauthorized());
        String resend = "/api/inscriptions/email/renvoyer";
        send(resend, Map.of("email", email, "password", PASSWORD)).andExpect(status().isAccepted());
        String fresh = sentLink();
        confirm(old, PASSWORD).andExpect(status().isUnauthorized());
        JsonNode first = setup(fresh);
        when(clock.instant()).thenReturn(NOW.plusSeconds(2400));
        activate(first, code(first, NOW.plusSeconds(2400))).andExpect(status().isUnauthorized());
        send(resend, Map.of("email", email, "password", PASSWORD)).andExpect(status().isAccepted());
        JsonNode second = setup(sentLink());
        assertNotEquals(first.get("manualKey"), second.get("manualKey"));
        activate(second, code(second, NOW.plusSeconds(2400))).andExpect(status().isOk());
    }

    @Test
    void lesEssaisMfaSontLimitesEtLaSuspensionExpire() throws Exception {
        send("/api/inscriptions", request("STUDENT")).andExpect(status().isAccepted());
        JsonNode setup = setup(sentLink());
        String invalid = "000000";
        var near = java.util.List.of(code(setup, NOW.minusSeconds(30)), code(setup, NOW), code(setup, NOW.plusSeconds(30)));
        for (int n = 1; near.contains(invalid); n++) {
            invalid = String.format(Locale.ROOT, "%06d", n);
        }
        for (int n = 0; n < 5; n++) activate(setup, invalid).andExpect(status().isUnauthorized());
        activate(setup, code(setup, NOW)).andExpect(status().isTooManyRequests());
        assertNull(candidate().getMfaActivatedAt());
        when(clock.instant()).thenReturn(NOW.plusSeconds(901));
        send("/api/inscriptions/email/renvoyer", Map.of("email", email, "password", PASSWORD)).andExpect(status().isAccepted());
        var recovered = setup(sentLink());
        activate(recovered, code(recovered, NOW.plusSeconds(901))).andExpect(status().isOk());
    }

    @Test
    void deuxActivationsSimultaneesNeConsommentLeJetonQuUneFois() throws Exception {
        send("/api/inscriptions", request("TEACHER")).andExpect(status().isAccepted());
        var setup = setup(sentLink());
        String otp = code(setup, NOW);
        var start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Integer> submit = () -> {
            start.await();
            return activate(setup, otp).andReturn().getResponse().getStatus();
        };
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(submit);
            var second = workers.submit(submit);
            start.countDown();
            var statuses = java.util.stream.Stream.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)).sorted().toList();
            assertEquals(java.util.List.of(200, 401), statuses);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void panneSmtpReprendSansCreerUnAutreCompteOuLien() throws Exception {
        send("/api/inscriptions", request("TEACHER")).andExpect(status().isAccepted());
        long id = candidate().getId();
        String digest = inscriptions.findById(id).orElseThrow().getEmailTokenHash();
        doThrow(new MailSendException("SMTP indisponible")).doNothing().when(smtp).send(any(SimpleMailMessage.class));
        mail.deliver(id);
        var waiting = inscriptions.findById(id).orElseThrow();
        assertEquals(NOW.plusSeconds(60), waiting.getNextMailAttemptAt());
        assertEquals(digest, waiting.getEmailTokenHash());
        when(clock.instant()).thenReturn(NOW.plusSeconds(61));
        setup(sentLink());
        assertNull(inscriptions.findById(id).orElseThrow().getNextMailAttemptAt());
    }
}
