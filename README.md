# SmartCampus

API Spring Boot 3.3 / Java 17 / PostgreSQL.

## Lancer en local

1. Créer la base PostgreSQL :

```sql
CREATE USER smartcampus WITH PASSWORD 'smartcampus_pwd';
CREATE DATABASE smartcampus OWNER smartcampus;
```

2. Démarrer avec le profil `dev` (secret JWT de dev + comptes de test) :

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Les tables sont créées automatiquement par Flyway au démarrage.
Hors profil `dev`, la variable `JWT_SECRET` (32 caractères minimum) est obligatoire, voir `.env.example`.

## Comptes de test (profil dev)

| Email                 | Mot de passe  | Rôle      |
|-----------------------|---------------|-----------|
| etudiant@usms.ac.ma   | Passw0rd!2026 | STUDENT   |
| professeur@usms.ac.ma | Prof!2026     | PROFESSOR |
| admin@usms.ac.ma      | Admin!2026    | ADMIN     |

## API

| Méthode | Route              | Accès        |
|---------|--------------------|--------------|
| POST    | /api/login         | public       |
| GET     | /api/health        | public       |
| GET     | /api/db            | public       |
| GET     | /api/me            | JWT          |
| GET     | /api/admin/users   | JWT + ADMIN  |

```bash
curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"email":"etudiant@usms.ac.ma","password":"Passw0rd!2026"}'

curl -s http://localhost:8080/api/me -H "Authorization: Bearer <token>"
```

Codes de `/api/login` : 200 succès, 400 requête invalide, 401 identifiants invalides,
403 compte désactivé, 423 compte verrouillé (5 échecs, 15 min par défaut).

## Migrations (Flyway)

- `src/main/resources/db/migration/` : schéma, appliqué partout (`V1__...`, `V2__...`).
- `src/main/resources/db/seed/` : données de test, profil `dev` uniquement.

Règle : ne jamais modifier une migration déjà mergée. Pour changer le schéma, ajouter
`V3__description.sql`, `V4__...`, etc.

## Tests

```bash
mvn test
```

Les tests utilisent H2 en mémoire (mode PostgreSQL) : aucune base externe n'est nécessaire.
