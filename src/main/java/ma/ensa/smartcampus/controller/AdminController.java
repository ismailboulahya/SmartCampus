package ma.ensa.smartcampus.controller;

import java.util.List;
import jakarta.validation.Valid;

import lombok.AllArgsConstructor;
import ma.ensa.smartcampus.dto.AccountFilter;
import ma.ensa.smartcampus.dto.AccountSummary;
import ma.ensa.smartcampus.dto.AccountStatusChange;
import ma.ensa.smartcampus.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminController {

    private final AccountService accounts;


    @GetMapping("/inscriptions")
    public List<AccountSummary> registrationsReadyForReview() {
        return accounts.registrationsReadyForReview();
    }

    @GetMapping("/users")
    public List<AccountSummary> users(@ModelAttribute AccountFilter filter) {
        return accounts.listAccounts(filter);
    }

    @PatchMapping("/users/{id}/statut")
    public AccountSummary changeStatus(@PathVariable long id, @Valid @RequestBody AccountStatusChange change,
            Authentication authentication) {
        return accounts.changeStatus(id, change, authentication.getName());
    }
}
