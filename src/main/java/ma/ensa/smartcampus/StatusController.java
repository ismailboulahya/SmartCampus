package ma.ensa.smartcampus;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final JdbcTemplate jdbc;

    public StatusController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        Integer r = jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("database", "UP", "result", r);
    }
}
