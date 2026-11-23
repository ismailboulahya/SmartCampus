package ma.ensa.smartcampus.controller;

import ma.ensa.smartcampus.dto.HealthResponse;
import ma.ensa.smartcampus.dto.DatabaseHealthResponse;
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
    public HealthResponse health() {
        return new HealthResponse("UP");
    }

    @GetMapping("/db")
    public DatabaseHealthResponse db() {
        Integer r = jdbc.queryForObject("SELECT 1", Integer.class);
        return new DatabaseHealthResponse("UP", r);
    }
}
