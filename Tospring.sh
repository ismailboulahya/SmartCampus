#!/usr/bin/env bash
# =============================================================
# tospring.sh : convertit SmartCampus en application Spring Boot
# en conservant LoginService (SC-11) et ses tests.
#
# Usage (depuis n'importe quel dossier du dépôt, Git Bash) :
#   bash tospring.sh
#
# Options :
#   SKIP_BUILD=1 bash tospring.sh     # ne lance pas Maven
#   BRANCH=autre-nom bash tospring.sh # change le nom de la branche
#
# Ne place PAS ce script dans src/ (il serait committé avec le code).
# Il ne fait ni commit ni push : tu gardes la main sur Git.
# =============================================================
set -euo pipefail

BRANCH="${BRANCH:-feature/SC-DOCKER}"
PKG_DIR="src/main/java/ma/ensa/smartcampus"

# --- 1. Se placer à la racine du dépôt ------------------------
ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || {
  echo "ERREUR : ce dossier n'est pas dans un dépôt Git." >&2
  exit 1
}
cd "$ROOT"
echo ">> Racine du dépôt : $ROOT"

# --- 2. Vérifications préalables ------------------------------
if [ ! -f "$PKG_DIR/auth/LoginService.java" ]; then
  echo "ERREUR : $PKG_DIR/auth/LoginService.java est introuvable." >&2
  echo "        Place-toi sur une branche qui contient SC-11." >&2
  exit 1
fi

if [ ! -f pom.xml ]; then
  echo "ATTENTION : aucun pom.xml existant, un nouveau sera créé."
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "ATTENTION : l'arbre de travail contient des modifications non committées :"
  git status --short
  read -r -p "Continuer quand même ? [o/N] " rep
  [ "$rep" = "o" ] || [ "$rep" = "O" ] || { echo "Abandon."; exit 1; }
fi

# --- 3. Branche dédiée ----------------------------------------
if git show-ref --verify --quiet "refs/heads/$BRANCH"; then
  git switch "$BRANCH"
else
  git switch -c "$BRANCH"
fi

mkdir -p src/main/resources src/test/java/ma/ensa/smartcampus

# --- 4. pom.xml (Spring Boot, mêmes coordonnées Maven) --------
echo ">> Écriture de pom.xml"
cat > pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
  </parent>

  <groupId>ma.ensa.smartcampus</groupId>
  <artifactId>SmartCampus</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <java.version>17</java.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
EOF

# --- 5. Classe principale Spring Boot -------------------------
echo ">> Écriture du code Java"
cat > "$PKG_DIR/SmartCampusApplication.java" << 'EOF'
package ma.ensa.smartcampus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartCampusApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartCampusApplication.class, args);
    }
}
EOF

# --- 6. Déclaration du LoginService comme bean (code inchangé) -
cat > "$PKG_DIR/AppConfig.java" << 'EOF'
package ma.ensa.smartcampus;

import ma.ensa.smartcampus.auth.LoginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public LoginService loginService() {
        return new LoginService();
    }
}
EOF

# --- 7. API de connexion (SC-11) ------------------------------
cat > "$PKG_DIR/AuthController.java" << 'EOF'
package ma.ensa.smartcampus;

import ma.ensa.smartcampus.auth.LoginResult;
import ma.ensa.smartcampus.auth.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(String email, String password) {}

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest request) {
        LoginResult result = loginService.login(request.email(), request.password());
        return ResponseEntity.status(result.success() ? 200 : 401).body(result);
    }
}
EOF

# --- 8. Endpoints de santé et de base de données --------------
cat > "$PKG_DIR/StatusController.java" << 'EOF'
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
EOF

# --- 9. Configuration (variables d'environnement) -------------
cat > src/main/resources/application.properties << 'EOF'
server.port=${SERVER_PORT:8080}
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/smartcampus}
spring.datasource.username=${DB_USER:smartcampus}
spring.datasource.password=${DB_PASSWORD:smartcampus_pwd}
EOF

# --- 10. Tests supplémentaires --------------------------------
cat > src/test/java/ma/ensa/smartcampus/ApiControllersTest.java << 'EOF'
package ma.ensa.smartcampus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ma.ensa.smartcampus.auth.LoginService;
import org.junit.jupiter.api.Test;

class ApiControllersTest {

    @Test
    void healthRetourneUP() {
        StatusController controller = new StatusController(null);
        assertEquals("UP", controller.health().get("status"));
    }

    @Test
    void loginAvecMauvaisMotDePasseRetourne401() {
        AuthController controller = new AuthController(new LoginService());
        var response = controller.login(
                new AuthController.LoginRequest("inconnu@usms.ac.ma", "mauvais-mot-de-passe"));
        assertEquals(401, response.getStatusCode().value());
        assertFalse(response.getBody().success());
    }
}
EOF

# Il ne doit y avoir qu'une seule classe main
OTHERS="$(grep -rl "public static void main" src/main/java \
          | grep -v SmartCampusApplication.java || true)"
if [ -n "$OTHERS" ]; then
  echo "ATTENTION : d'autres classes contiennent un main, à supprimer :"
  echo "$OTHERS"
fi

# --- 11. Construction -----------------------------------------
if [ "${SKIP_BUILD:-0}" = "1" ]; then
  echo ">> SKIP_BUILD=1 : construction ignorée."
else
  # Utiliser un JDK 17+ si JAVA_HOME n'est pas défini (Windows / Git Bash)
  if [ -z "${JAVA_HOME:-}" ]; then
    for d in "/c/Program Files/Java/jdk-21" "/c/Program Files/Java/jdk-23" \
             "/c/Program Files/Java/jdk-25"; do
      if [ -x "$d/bin/javac.exe" ] || [ -x "$d/bin/javac" ]; then
        export JAVA_HOME="$d"
        export PATH="$JAVA_HOME/bin:$PATH"
        break
      fi
    done
  fi
  echo ">> JAVA_HOME = ${JAVA_HOME:-non défini}"
  echo ">> Construction : mvn -B clean package"
  mvn -B clean package

  JAR="$(ls target/SmartCampus-*.jar 2>/dev/null | grep -v original | head -n 1 || true)"
  if [ -n "$JAR" ]; then
    echo ">> Jar produit : $JAR"
    unzip -p "$JAR" META-INF/MANIFEST.MF | grep -i "main-class" \
      || echo "ATTENTION : aucun Main-Class dans le manifeste."
  fi
fi

# --- 12. Suite ------------------------------------------------
cat << 'EOF'

=============================================================
Terminé. Étapes suivantes (manuelles) :

  1. Lancer l'application (avec un JDK 17+, pas le Java 8) :
       "$JAVA_HOME/bin/java" -jar target/SmartCampus-1.0-SNAPSHOT.jar

  2. Tester :
       curl http://localhost:8080/api/health
       curl -i -X POST http://localhost:8080/api/login \
         -H "Content-Type: application/json" \
         -d '{"email":"etudiant@usms.ac.ma","password":"faux"}'

  3. Committer et pousser :
       git status
       git add pom.xml src/
       git commit -m "SC-DOCKER Expose LoginService par une API Spring Boot"
       git push -u origin feature/SC-DOCKER
=============================================================
EOF
