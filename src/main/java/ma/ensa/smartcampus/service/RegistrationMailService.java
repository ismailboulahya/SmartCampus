package ma.ensa.smartcampus.service;

import java.time.Clock;
import java.time.Instant;
import ma.ensa.smartcampus.entity.AccountStatus;
import ma.ensa.smartcampus.repository.InscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationMailService {
    private static final Logger LOG = LoggerFactory.getLogger(RegistrationMailService.class);
    private final InscriptionRepository inscriptions;
    private final EnrollmentSecrets secrets;
    private final JavaMailSender mail;
    private final Clock clock;
    private final String from;
    private final String confirmationPage;

    public RegistrationMailService(InscriptionRepository inscriptions, EnrollmentSecrets secrets, JavaMailSender mail,
            Clock clock, @Value("${app.registration.from}") String from,
            @Value("${app.registration.public-url}") String baseUrl) {
        this.inscriptions = inscriptions;
        this.secrets = secrets;
        this.mail = mail;
        this.clock = clock;
        this.from = from;
        this.confirmationPage = baseUrl.replaceAll("/+$", "") + "/inscription.html#";
    }

    @Transactional
    public void deliver(long userId) {
        var entry = inscriptions.lockByUserId(userId).orElseThrow();
        Instant now = clock.instant();
        if (entry.getNextMailAttemptAt() == null || now.isBefore(entry.getNextMailAttemptAt())) {
            return;
        }
        if (entry.getUser().getStatus() == AccountStatus.EN_ATTENTE && now.isBefore(entry.getEmailExpiresAt())) {
            String token = secrets.decrypt(entry.getEncryptedMailToken(), "mail:" + userId);
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(entry.getUser().getEmail());
            message.setSubject("SmartCampus – vérification de votre adresse");
            message.setText("Pour poursuivre votre inscription, ouvrez ce lien puis confirmez votre mot de passe.\n\n"
                    + confirmationPage + token + "\n\nSi vous n’avez pas demandé cette inscription, ignorez ce message.");
            try {
                mail.send(message);
            } catch (MailException ex) {
                entry.setNextMailAttemptAt(now.plusSeconds(60));
                LOG.warn("Envoi SMTP différé pour le dossier {} ({})", userId, ex.getClass().getSimpleName());
                return;
            }
        }
        entry.setEncryptedMailToken(null);
        entry.setNextMailAttemptAt(null);
    }
}
