package ma.ensa.smartcampus.config;

import ma.ensa.smartcampus.service.AccountCreationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoAccountsConfig {

    @Bean
    CommandLineRunner demoAccounts(AccountCreationService accounts, @Value("${app.demo.password}") String password) {
        return args -> accounts.createDemoAccounts(password);
    }
}
