package ma.ensa.smartcampus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String path : new String[]{"/", "/admin", "/connexion", "/inscription", "/inscription.html"}) {
            registry.addViewController(path).setViewName("forward:/index.html");
        }
    }
}
