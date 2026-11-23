# Inscription multifacteur

L’étudiant ou l’enseignant ouvre `/inscription.html`, dépose son dossier puis suit le lien reçu à son adresse `@usms.ac.ma`. Il confirme le mot de passe de l’inscription, ajoute le compte dans son application d’authentification (lien ou saisie de la clé) et saisit le code à six chiffres. Sa demande complète devient visible sur `GET /api/admin/inscriptions`. Son statut reste `EN_ATTENTE` et aucun jeton de connexion n’est émis.

## Essai local avec Docker Compose

1. Fournir les variables PostgreSQL et JWT du projet. Ajouter `REGISTRATION_ENCRYPTION_KEY` (32 octets aléatoires en Base64) à l’environnement, ou au fichier local `.env`. Conserver cette clé entre deux démarrages et sur toutes les instances. PowerShell 7 permet de la générer pour la session :

   ```powershell
   $env:REGISTRATION_ENCRYPTION_KEY = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

2. Exécuter `mvn verify`, puis `docker compose up --build`. Le Dockerfile utilise le JAR produit par Maven.
3. Ouvrir `http://localhost:8080/inscription.html`. Dans cette configuration locale, les courriels sont capturés par Mailpit et consultables sur `http://localhost:8025`. Pour exécuter uniquement la boîte locale avec l’API lancée par Maven, utiliser `docker compose up mailpit` et le port SMTP local 1025.

Avec un relais institutionnel, définir `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` et `SMTP_STARTTLS` selon le relais. Définir également l’expéditeur `REGISTRATION_FROM` et l’origine HTTPS de l’application `REGISTRATION_PUBLIC_URL`. La clé de chiffrement est distincte de `JWT_SECRET`.

## Recevoir le courriel de test dans Outlook

Pour tester avec ta propre boîte universitaire, crée un compte Resend avec la même adresse `@usms.ac.ma` que celle du dossier SmartCampus et crée une clé de type **Sending access**. Le domaine d’essai `resend.dev` autorise l’envoi à l’adresse associée à ton compte Resend. [Règle du domaine de test](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain)

Depuis le dossier du projet, lance ces deux commandes sous PowerShell 7. La première demande la clé avec saisie masquée. La seconde applique au service API la configuration SMTP avec STARTTLS. [Paramètres SMTP de Resend](https://resend.com/docs/send-with-smtp)

```powershell
$env:RESEND_API_KEY = Read-Host 'Clé API Resend' -MaskInput
docker compose -f compose.yaml -f compose.resend.yaml up -d --no-deps --wait api
```

Ouvre le [formulaire local](http://localhost:8080/inscription.html). Pour un dossier déjà créé dont le second facteur reste à associer, utilise **Recevoir un nouveau lien** avec la même adresse et le mot de passe choisi à l’inscription. Pour une première demande, complète le formulaire. Ouvre le lien reçu dans Outlook sur le PC où tourne SmartCampus, puisque l’adresse du lien commence par `localhost`.

La variable PowerShell vaut pour la session en cours. Pour conserver la configuration, tu peux définir `RESEND_API_KEY` dans ton fichier `.env` local. Utilise les deux fichiers Compose pour les démarrages avec Resend. Pour rétablir la livraison vers Mailpit avec les réglages locaux par défaut, exécute `docker compose -f compose.yaml up -d --no-deps api`.

## Contrat des routes

| Route | Entrée | Résultat |
| --- | --- | --- |
| `POST /api/inscriptions` | Identité, profil universitaire et mot de passe | `202` générique. Le compte et l’envoi à effectuer sont enregistrés ensemble. Un doublon conserve le premier dossier. |
| `POST /api/inscriptions/email/renvoyer` | `email`, `password` | `202` générique. Pour un dossier admissible, un nouveau lien remplace le précédent. Délai minimal d’une minute. |
| `POST /api/inscriptions/email/confirmer` | `token` du lien et `password` | Date la vérification, consomme le lien et fournit `manualKey`, `authenticatorUri`, `setupToken` et `expiresAt`. |
| `POST /api/inscriptions/mfa/activer` | `setupToken` et `code` (chaîne de six chiffres) | Consomme le jeton d’enrôlement et date l’activation du facteur. |
| `GET /api/admin/inscriptions` | JWT d’un administrateur actif | Liste des comptes en attente dont l’adresse et le facteur sont vérifiés. |

Exemple de demande étudiante :

```json
{
  "role": "STUDENT",
  "firstName": "Lina",
  "lastName": "Exemple",
  "email": "lina.exemple@usms.ac.ma",
  "password": "exemple-local-uniquement",
  "studentProfile": { "cne": "CNE-001", "filiere": "MGSI" }
}
```

Pour un enseignant, utiliser `"role": "TEACHER"` et `"teacherProfile": { "matricule": "MAT-001", "departement": "Informatique" }`. Fournir exactement un profil. Le mot de passe comporte au moins huit caractères et au plus 72 octets UTF-8 pour BCrypt.

## Validité et reprise

- Le lien d’e-mail expire après 30 minutes. Ouvrir la page ne consomme pas le lien. Le mot de passe et une confirmation explicite sont nécessaires.
- L’association TOTP dispose ensuite de dix minutes. Le code change toutes les 30 secondes. La vérification tolère un intervalle d’écart dans chaque sens.
- Cinq preuves incorrectes suspendent la vérification pendant quinze minutes. Les compteurs et les échéances sont conservés en base.
- Si la page est fermée avant la fin, utiliser « Recevoir un nouveau lien ». Une nouvelle confirmation remplace le facteur provisoire. Après l’activation, ce parcours ne permet plus de remplacer le facteur.
- Si le relais SMTP est indisponible, l’envoi est repris après une minute, tant que le lien reste valide. La migration V3 laisse les dates de vérification des comptes historiques vides.
- Le second facteur est établi à l’inscription. Un administrateur actif peut ensuite traiter le dossier dans l’[espace d’administration](http://localhost:8080/admin).
