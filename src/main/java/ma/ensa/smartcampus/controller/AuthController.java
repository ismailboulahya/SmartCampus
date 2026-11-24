package ma.ensa.smartcampus.controller;

import jakarta.validation.Valid;
import ma.ensa.smartcampus.dto.ApiMessage;
import ma.ensa.smartcampus.dto.AccountDetails;
import ma.ensa.smartcampus.dto.LoginRequest;
import ma.ensa.smartcampus.dto.LoginResponse;
import ma.ensa.smartcampus.service.AccountService;
import ma.ensa.smartcampus.service.LoginService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final LoginService loginService;
    private final AccountService accounts;

    public AuthController(LoginService loginService, AccountService accounts) {
        this.loginService = loginService;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request.email(), request.password());
    }

    @GetMapping("/me")
    public AccountDetails me(Authentication authentication) {
        return accounts.currentAccount(authentication.getName());
    }

    @PostMapping("/logout")
    public ApiMessage logout() {
        return new ApiMessage(true, "Déconnexion réussie.");
    }
}
