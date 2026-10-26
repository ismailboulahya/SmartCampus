package ma.ensa.smartcampus;

import java.time.Clock;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan
public class AppConfig {

    // LoginService est maintenant un @Service (ma.ensa.smartcampus.auth) :
    // il n'est plus déclaré ici.

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
