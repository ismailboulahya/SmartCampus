import { useCallback, useState } from "react";
import { api } from "./api";
import Dashboard from "./Admin";

function Login({ onLogin, admin }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function login(event) {
    event.preventDefault();
    const body = Object.fromEntries(new FormData(event.currentTarget));
    setBusy(true);
    setError("");
    try {
      const session = await api("/login", { method: "POST", body });
      if (admin && session.role !== "ADMIN")
        throw new Error("Cet espace est réservé aux administrateurs.");
      const me = await api("/me", { token: session.token });
      onLogin({ token: session.token, me });
    } catch (err) {
      setError(
        err.status === 401
          ? "Connexion refusée. Vérifiez vos identifiants et l’activation du compte."
          : err.message,
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="card login">
      <h1>{admin ? "Administration" : "Connexion"}</h1>
      <p>
        {admin
          ? "Connectez-vous avec votre compte administrateur actif."
          : "Accédez à votre compte universitaire."}
      </p>
      <form onSubmit={login}>
        <label>
          Adresse institutionnelle
          <input name="email" type="email" autoComplete="username" required autoFocus />
        </label>
        <label>
          Mot de passe
          <input name="password" type="password" autoComplete="current-password" required />
        </label>
        {error && (
          <p className="notice error" role="alert">
            {error}
          </p>
        )}
        <button className="primary" disabled={busy}>
          {busy ? "Connexion…" : "Se connecter"}
        </button>
      </form>
    </section>
  );
}

export default function SessionPage({ admin }) {
  const [session, setSession] = useState(null);
  const [message, setMessage] = useState("");
  const endSession = useCallback((reason) => {
    setMessage(reason);
    setSession(null);
  }, []);
  async function logout() {
    try {
      await api("/logout", { method: "POST", token: session.token });
    } catch {
      /* The local session also ends when the API is unavailable. */
    }
    endSession("");
  }

  if (session?.me.role === "ADMIN")
    return <Dashboard session={session} endSession={endSession} logout={logout} />;
  if (session) {
    const me = session.me;
    const academic = me.studentProfile || me.teacherProfile;
    return (
      <section className="card login">
        <h1>Mon compte</h1>
        <p>{[me.firstName, me.lastName].filter(Boolean).join(" ")}</p>
        <dl>
          <dt>Adresse institutionnelle</dt>
          <dd>{me.email}</dd>
          <dt>Profil</dt>
          <dd>{me.role === "TEACHER" ? "Enseignant" : "Étudiant"}</dd>
          {academic && (
            <>
              <dt>{me.studentProfile ? "Filière" : "Département"}</dt>
              <dd>{academic.filiere || academic.departement}</dd>
              <dt>{me.studentProfile ? "CNE" : "Matricule"}</dt>
              <dd>{academic.cne || academic.matricule}</dd>
            </>
          )}
        </dl>
        <button className="secondary" onClick={logout}>
          Se déconnecter
        </button>
      </section>
    );
  }
  return (
    <>
      {message && (
        <p role="status" className="session-msg">
          {message}
        </p>
      )}
      <Login
        admin={admin}
        onLogin={(value) => {
          setMessage("");
          setSession(value);
        }}
      />
    </>
  );
}
