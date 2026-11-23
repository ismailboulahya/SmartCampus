package ma.ensa.smartcampus.service;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import ma.ensa.smartcampus.repository.InscriptionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.registration.mail-worker-enabled", matchIfMissing = true)
public class RegistrationMailWorker {
    private final InscriptionRepository inscriptions;
    private final RegistrationMailService mail;
    private final Clock clock;

    @Scheduled(fixedDelay = 5000)
    public void deliverPending() {
        for (long id : inscriptions.dueForMail(clock.instant(), PageRequest.of(0, 25))) {
            mail.deliver(id);
        }
    }
}
