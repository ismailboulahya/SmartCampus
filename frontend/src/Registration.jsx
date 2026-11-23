import { useState } from "react";
import { api } from "./api";

function Field({ label, ...props }) {
  return (
    <label>
      {label}
      <input required maxLength={255} {...props} />
    </label>
  );
}

function Password({ fresh = false }) {
  return (
    <Field
      label="Mot de passe"
      name="password"
      type="password"
      autoComplete={fresh ? "new-password" : "current-password"}
      minLength={fresh ? 8 : undefined}
      maxLength={72}
    />
  );
}

function RequestForm({ onSubmit, button, children }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = Object.fromEntries(new FormData(form));
    setBusy(true);
    setError("");
    try {
      await onSubmit(values);
      form.reset();
    } catch (err) {
      setError(err instanceof TypeError ? "Connexion indisponible. Réessayez." : err.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      {children}
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      <button className="primary" disabled={busy}>
        {busy ? "En cours…" : button}
      </button>
    </form>
  );
}

function Signup({ onSent }) {
  const [role, setRole] = useState("STUDENT");
  async function register({
    firstName,
    lastName,
    email,
    password,
    cne,
    filiere,
    matricule,
    departement,
  }) {
    const profile =
      role === "STUDENT"
        ? { studentProfile: { cne, filiere } }
        : { teacherProfile: { matricule, departement } };
    const reply = await api("/inscriptions", {
      method: "POST",
      body: { role, firstName, lastName, email, password, ...profile },
    });
    onSent(reply.message);
  }
  return (
    <RequestForm onSubmit={register} button="Demander mon inscription">
      <fieldset>
        <legend>Profil universitaire</legend>
        <div className="roles">
          {[
            ["STUDENT", "Étudiant"],
            ["TEACHER", "Enseignant"],
          ].map(([value, name]) => (
            <label key={value}>
              <input
                type="radio"
                name="role"
                value={value}
                checked={role === value}
                onChange={() => setRole(value)}
              />
              {name}
            </label>
          ))}
        </div>
      </fieldset>
      <div className="pair">
        <Field label="Prénom" name="firstName" autoComplete="given-name" />
        <Field label="Nom" name="lastName" autoComplete="family-name" />
      </div>
      <Field
        label="Adresse institutionnelle"
        name="email"
        type="email"
        placeholder="prenom.nom@usms.ac.ma"
        autoComplete="email"
      />
      <div className="pair" key={role}>
        {role === "STUDENT" ? (
          <>
            <Field label="CNE" name="cne" />
            <Field label="Filière" name="filiere" />
          </>
        ) : (
          <>
            <Field label="Matricule" name="matricule" />
            <Field label="Département" name="departement" />
          </>
        )}
      </div>
      <Password fresh />
    </RequestForm>
  );
}

const titles = {
  register: "Créer mon compte",
  sent: "Consultez votre messagerie",
  confirm: "Vérifier mon adresse",
  mfa: "Associer mon application",
  done: "Dossier complet",
};

export default function Registration({ initialToken }) {
  const [stage, setStage] = useState(initialToken ? "confirm" : "register");
  const [setup, setSetup] = useState(null);
  const [message, setMessage] = useState("");
  const current = { register: 0, sent: 1, confirm: 1, mfa: 2, done: 3 }[stage];
  function sent(text) {
    setSetup(null);
    setMessage(text);
    setStage("sent");
  }
  async function confirm({ password }) {
    const result = await api("/inscriptions/email/confirmer", {
      method: "POST",
      body: { token: initialToken, password },
    });
    setSetup(result);
    setStage("mfa");
  }
  async function activate({ code }) {
    const result = await api("/inscriptions/mfa/activer", {
      method: "POST",
      body: { setupToken: setup.setupToken, code },
    });
    setMessage(result.message);
    setSetup(null);
    setStage("done");
  }
  return (
    <div className="form-layout">
      <aside className="journey">
        <h2>Votre inscription</h2>
        <ol>
          {["Profil universitaire", "Adresse institutionnelle", "Double authentification"].map(
            (label, index) => (
              <li
                key={label}
                className={index < current ? "complete" : ""}
                aria-current={index === current ? "step" : undefined}
              >
                <span aria-hidden="true">{index < current ? "✓" : index + 1}</span>
                {label}
              </li>
            ),
          )}
        </ol>
      </aside>
      <section className="card" key={stage}>
        <h1>{titles[stage]}</h1>
        {stage === "register" && <Signup onSent={sent} />}
        {stage === "confirm" && (
          <>
            <p>Confirmez le mot de passe choisi à l’inscription.</p>
            <RequestForm onSubmit={confirm} button="Confirmer mon adresse">
              <Password />
            </RequestForm>
          </>
        )}
        {stage === "mfa" && (
          <>
            <p>
              Ajoutez SmartCampus à votre application d’authentification avec cette clé ou ouvrez
              directement l’application.
            </p>
            <code className="secret">{setup.manualKey}</code>
            <a href={setup.authenticatorUri}>Ouvrir l’application</a>
            <RequestForm onSubmit={activate} button="Terminer mon inscription">
              <Field
                label="Code à six chiffres"
                name="code"
                inputMode="numeric"
                pattern="[0-9]{6}"
                maxLength={6}
                autoComplete="one-time-code"
              />
            </RequestForm>
          </>
        )}
        {(stage === "sent" || stage === "done") && <p role="status">{message}</p>}
        {stage !== "done" && (
          <details className="recovery">
            <summary>Recevoir un nouveau lien</summary>
            <RequestForm
              button="Renvoyer le lien"
              onSubmit={async (body) => {
                const result = await api("/inscriptions/email/renvoyer", { method: "POST", body });
                sent(result.message);
              }}
            >
              <Field label="Adresse institutionnelle" name="email" type="email" autoComplete="email" />
              <Password />
            </RequestForm>
          </details>
        )}
      </section>
    </div>
  );
}
