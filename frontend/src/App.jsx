import { useAuth } from './context/AuthContext.jsx';
import LoginPage from './pages/LoginPage.jsx';

export default function App() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <main className="login-page"><p>Chargement...</p></main>;
  }

  if (!user) {
    return <LoginPage />;
  }

  const nomComplet = [user.firstName, user.lastName].filter(Boolean).join(' ');

  // Placeholder : les espaces étudiant et admin seront ajoutés à l'étape suivante.
  return (
    <main className="login-page">
      <div className="login-card">
        <h1>Bienvenue</h1>
        <p className="login-subtitle">{nomComplet || user.email} · {user.role}</p>
        <button onClick={logout}>Se déconnecter</button>
      </div>
    </main>
  );
}
