# Comptes et profils

Un étudiant possède un CNE et une filière, un enseignant un matricule et un département. Les rôles JSON sont `STUDENT`, `TEACHER` et `ADMIN`. Un compte créé dans le modèle commence en `EN_ATTENTE`. Seul `ACTIF` permet la connexion et les appels protégés. Le [parcours d’inscription](inscription.md) vérifie l’adresse et le second facteur.

## Interface React et API REST

Le projet React se trouve dans `frontend/`. Maven utilise Node.js 22.12+ et npm pour installer les dépendances verrouillées, compiler le frontend avec Vite et inclure les fichiers dans le JAR Spring Boot. Les pages et les requêtes JSON vers `/api` partagent la même origine. Dans le navigateur, la session JWT est conservée en mémoire jusqu’au rechargement ou à la déconnexion.

| Page | Accès et usage |
| --- | --- |
| [Inscription](http://localhost:8080/inscription) | Création du dossier, lien de vérification et association du second facteur. Les liens déjà émis vers `/inscription.html` fonctionnent également. |
| [Connexion](http://localhost:8080/connexion) | Connexion et consultation du profil du titulaire. |
| [Administration](http://localhost:8080/admin) | Connexion avec un compte administrateur actif, traitement des dossiers et gestion des comptes. |

Pour travailler sur React avec rechargement automatique, laisse l’API en marche sur le port 8080 et exécute `npm ci` puis `npm run dev` dans `frontend/`. Utilise l’URL locale indiquée par Vite. Le proxy transmet `/api` à Spring Boot. Pour déployer, exécute `mvn clean verify` puis `docker compose up -d --build`. Si l’instance utilise Resend, conserve les deux fichiers avec `docker compose -f compose.yaml -f compose.resend.yaml up -d --build`.

## Modèle JPA

`Utilisateur` est la classe abstraite commune aux entités `Etudiant`, `Enseignant` et `Administrateur`. Le mapping JPA `SINGLE_TABLE` réutilise la table `users` et sa colonne `role` comme discriminant. Les lignes existantes sont chargées dans le sous-type correspondant, sans migration supplémentaire après V2.

Lombok génère les accesseurs. Les services construisent les comptes, normalisent l’adresse et préparent les DTO, définis comme des records Java.

## Essayer sur une base de démonstration

Utiliser une base PostgreSQL dédiée et définir `DB_URL`, `DB_USER`, `DB_PASSWORD` et les clés `REGISTRATION_ENCRYPTION_KEY` (32 octets en Base64) et `JWT_SECRET` (clé aléatoire d’au moins 32 octets). Activer le profil Spring `demo` et définir `APP_DEMO_PASSWORD` dans l’environnement avant de lancer `mvn spring-boot:run`.

Avec PowerShell, une fois les variables de connexion et les secrets définis :

```powershell
$env:SPRING_PROFILES_ACTIVE = 'demo'
mvn spring-boot:run
```

Sur une base de démonstration vide, ce profil crée trois comptes actifs, `etudiant@usms.ac.ma`, `enseignant@usms.ac.ma` et `admin@usms.ac.ma`. Le mot de passe de démonstration est celui fourni au démarrage. Si une adresse existe déjà, son statut, son profil et ses identifiants sont conservés.

| Route | Accès | Réponse |
| --- | --- | --- |
| `POST /api/login` | E-mail institutionnel et mot de passe d’un compte actif | Jeton JWT et rôle |
| `GET /api/me` | Jeton d’un compte actif | Identité, rôle, statut, date de création et profil universitaire du titulaire |
| `GET /api/admin/users` | Jeton d’un administrateur actif | Liste des identifiants, e-mails, rôles et statuts |
| `GET /api/admin/inscriptions` | Jeton d’un administrateur actif | Dossiers en attente avec adresse et facteur vérifiés |
| `PATCH /api/admin/users/{id}/statut` | Jeton d’un administrateur actif | Modifie le statut après vérification de l’état annoncé et renvoie le compte actualisé |

Le corps JSON d’une approbation est `{"from":"EN_ATTENTE","to":"ACTIF"}`. Un dossier complet peut aussi passer à `REFUSE`. Le blocage fait passer le compte de `ACTIF` à `BLOQUE` et le déblocage de `BLOQUE` à `ACTIF`. Un changement concurrent ou une décision incompatible renvoie HTTP 409. Un administrateur ne peut pas modifier son propre statut par cette route.

La recherche administrative accepte les paramètres facultatifs `role` (`STUDENT`, `TEACHER`, `ADMIN`), `status` (les quatre statuts du compte) et `email` (adresse exacte, espaces de bord et casse ignorés). Les critères sont combinés avec ET via une `Specification<Utilisateur>`. Sans filtre, la route retourne tous les comptes dans l’ordre des identifiants.

Exemple : `GET /api/admin/users?role=TEACHER&status=ACTIF`.

## Reprendre une base existante

Flyway reconnaît l’ancien schéma SmartCampus comme version 1 et applique la migration V2. Utiliser une base ou un schéma dédié à SmartCampus. La migration conserve les identifiants, les hachages et les rôles. Elle normalise les e-mails et place **tous les comptes existants, administrateurs compris**, en `EN_ATTENTE`. Les dates de création et les données universitaires historiques inconnues restent nulles.

Avant la mise à niveau, inventorier les anciens rôles et vérifier que deux adresses distinctes ne deviennent pas identiques après normalisation. Une collision ou un rôle inconnu demande une décision sur les données avant de pouvoir terminer la migration.

```sql
SELECT role, COUNT(*) FROM users GROUP BY role;
SELECT LOWER(TRIM(email)), COUNT(*) FROM users
GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
```

Prévoir la reprise du premier administrateur avec le responsable de la base. Après vérification de son identité, l’opérateur habilité peut activer ce compte précis. Pour les comptes universitaires repris, compléter le CNE et la filière ou le matricule et le département avant activation. Le profil de démonstration doit être réservé à une base d’essai.

## Vérification locale

`mvn verify` compile le code, applique les migrations sur H2 en mode PostgreSQL et exécute les tests de migration, de connexion et d’accès aux profils.
