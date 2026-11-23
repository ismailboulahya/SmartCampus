import { useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';

const DOMAINE = '@usms.ac.ma';

function messageErreur(result) {
  if (!result) return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
  if (result.status === 429) return 'Trop de tentatives, réessayez plus tard.';
  if (result.status === 401) return 'Identifiants invalides.';
  if (result.status === 400) return 'Requête invalide. Vérifiez les champs saisis.';
  return result.data?.message || 'Une erreur est survenue. Réessayez.';
}

export default function LoginPage() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [erreur, setErreur] = useState('');
  const [chargement, setChargement] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setErreur('');

    const emailNormalise = email.trim().toLowerCase();
    if (!emailNormalise.endsWith(DOMAINE)) {
      setErreur(`L'adresse doit se terminer par ${DOMAINE}.`);
      return;
    }
    if (!password) {
      setErreur('Veuillez saisir votre mot de passe.');
      return;
    }

    setChargement(true);
    try {
      const result = await login(emailNormalise, password);
      if (!result.ok || !result.data?.success) {
        setErreur(messageErreur(result));
      }
    } catch {
      setErreur(messageErreur(null));
    } finally {
      setChargement(false);
    }
  }

  return (
    <main className="login-page">
      <form className="login-card" onSubmit={handleSubmit} noValidate>
        <h1>SmartCampus</h1>
        <p className="login-subtitle">Connectez-vous avec votre compte institutionnel</p>

        <label htmlFor="email">Adresse email</label>
        <input
          id="email"
          type="email"
          autoComplete="username"
          placeholder={`prenom.nom${DOMAINE}`}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={chargement}
        />

        <label htmlFor="password">Mot de passe</label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={chargement}
        />

        {erreur && <p className="login-error" role="alert">{erreur}</p>}

        <button type="submit" disabled={chargement}>
          {chargement ? 'Connexion...' : 'Se connecter'}
        </button>
      </form>
    </main>
  );
}
