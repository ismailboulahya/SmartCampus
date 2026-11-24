package ma.ensa.smartcampus.controller;

import ma.ensa.smartcampus.dto.DatabaseHealthResponse;
import ma.ensa.smartcampus.dto.HealthResponse;
import ma.ensa.smartcampus.dto.ReadinessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /** Sonde de vivacité : répond tant que l'application est en vie. */
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }

    /** Sonde de disponibilité : échoue si la base de données est injoignable. */
    @GetMapping("/health/ready")
    public ResponseEntity<ReadinessResponse> ready() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok(new ReadinessResponse("UP", "UP"));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ReadinessResponse("DOWN", "DOWN"));
        }
    }

    @GetMapping("/db")
    public DatabaseHealthResponse db() {
        Integer r = jdbc.queryForObject("SELECT 1", Integer.class);
        return new DatabaseHealthResponse("UP", r);
    }
}
