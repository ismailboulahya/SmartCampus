package ma.ensa.smartcampus.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensa.smartcampus.dto.*;
import ma.ensa.smartcampus.exception.DuplicateRegistrationException;
import ma.ensa.smartcampus.service.RegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
public class RegistrationController {
    private static final ApiMessage RECEIVED = new ApiMessage(true,
            "Si votre demande peut être traitée, vous recevrez un lien à votre adresse institutionnelle.");
    private final RegistrationService registrations;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiMessage register(@Valid @RequestBody RegistrationRequest request) {
        registrations.register(request);
        return RECEIVED;
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiMessage duplicateRegistration() {
        return RECEIVED;
    }

    @PostMapping("/email/renvoyer")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiMessage resend(@Valid @RequestBody LoginRequest request) {
        registrations.resend(request);
        return RECEIVED;
    }

    @PostMapping("/email/confirmer")
    public MfaSetup confirmEmail(@Valid @RequestBody EmailConfirmation request) {
        return registrations.confirmEmail(request);
    }

    @PostMapping("/mfa/activer")
    public ApiMessage activate(@Valid @RequestBody MfaActivation request) {
        registrations.activate(request);
        return new ApiMessage(true, "Dossier complet. Votre inscription est en attente de la décision administrative.");
    }
}
