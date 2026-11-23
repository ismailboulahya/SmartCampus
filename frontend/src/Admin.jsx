import { useEffect, useState } from "react";
import { api } from "./api";

const roles = { STUDENT: "Étudiant", TEACHER: "Enseignant", ADMIN: "Administrateur" };
const statuses = { EN_ATTENTE: "En attente", ACTIF: "Actif", REFUSE: "Refusé", BLOQUE: "Bloqué" };

const emptyFilters = { role: "", status: "", email: "" };

function Filters({ apply }) {
  return (
    <form
      className="filters"
      onSubmit={(event) => {
        event.preventDefault();
        apply(Object.fromEntries(new FormData(event.currentTarget)));
      }}
    >
      <label>
        Adresse exacte
        <input type="email" name="email" placeholder="Adresse institutionnelle" />
      </label>
      <label>
        Profil
        <select name="role">
          <option value="">Tous les profils</option>
          {Object.entries(roles).map(([key, label]) => (
            <option key={key} value={key}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Statut
        <select name="status">
          <option value="">Tous les statuts</option>
          {Object.entries(statuses).map(([key, label]) => (
            <option key={key} value={key}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <button className="secondary">Filtrer</button>
    </form>
  );
}

export default function Dashboard({ session, endSession, logout }) {
  const [tab, setTab] = useState("review");
  const [filters, setFilters] = useState(emptyFilters);
  const [revision, setRevision] = useState(0);
  const [rows, setRows] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [decision, setDecision] = useState(null);
  const [saving, setSaving] = useState(false);
  const review = tab === "review";

  useEffect(() => {
    const aborter = new AbortController();
    const query = new URLSearchParams(Object.entries(filters).filter(([, value]) => value));
    const path = review ? "/admin/inscriptions" : "/admin/users?" + query;
    setRows(null);
    setError("");
    api(path, { token: session.token, signal: aborter.signal })
      .then((data) => {
        if (!aborter.signal.aborted) setRows(data);
      })
      .catch((err) => {
        if (aborter.signal.aborted) return;
        if (err.status === 401 || err.status === 403)
          endSession("Votre accès a expiré. Reconnectez-vous.");
        else setError(err.message);
      });
    return () => aborter.abort();
  }, [review, filters, revision, session.token, endSession]);

  function selectTab(value) {
    setTab(value);
    setFilters(emptyFilters);
    setDecision(null);
    setNotice("");
  }
  function choose(row, to, label) {
    setDecision({ row, to, label });
    setNotice("");
    setError("");
  }
  async function confirm() {
    setSaving(true);
    const { row, to } = decision;
    try {
      const updated = await api("/admin/users/" + row.id + "/statut", {
        method: "PATCH",
        token: session.token,
        body: { from: row.status, to },
      });
      setNotice(updated.email + " · " + statuses[updated.status]);
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        endSession("Votre accès a expiré. Reconnectez-vous.");
        return;
      }
      setNotice(err.message);
    } finally {
      setSaving(false);
      setDecision(null);
      setRevision((n) => n + 1);
    }
  }
  return (
    <section className="dashboard">
      <div className="dash-head">
        <h1>Administration</h1>
        <div className="identity">
          <span>{session.me.email}</span>
          <button className="link-btn" onClick={logout}>
            Se déconnecter
          </button>
        </div>
      </div>
      <div className="tabs" aria-label="Vue administrative">
        <button aria-pressed={review} onClick={() => selectTab("review")} disabled={saving}>
          Inscriptions à traiter
        </button>
        <button aria-pressed={!review} onClick={() => selectTab("users")} disabled={saving}>
          Comptes utilisateurs
        </button>
      </div>
      <div className="list-panel">
        <div className="list-head">
          <h2>{review ? "Dossiers complets" : "Utilisateurs"}</h2>
          <button
            className="secondary"
            disabled={saving}
            onClick={() => {
              setDecision(null);
              setRevision((n) => n + 1);
            }}
          >
            Actualiser
          </button>
        </div>
        {!review && (
          <Filters
            apply={(value) => {
              setFilters(value);
              setDecision(null);
              setNotice("");
            }}
          />
        )}
        {notice && (
          <p role="status" className="notice">
            {notice}
          </p>
        )}
        {error && (
          <p role="alert" className="notice error">
            {error}
          </p>
        )}
        {decision && (
          <div className="confirm-action" role="group" aria-label="Confirmer la décision">
            <p>
              <strong>{decision.label}</strong> · {decision.row.email}
            </p>
            <div className="action-group">
              <button className="primary" onClick={confirm} disabled={saving}>
                Confirmer
              </button>
              <button className="secondary" onClick={() => setDecision(null)} disabled={saving}>
                Annuler
              </button>
            </div>
          </div>
        )}
        {!error && rows === null && <p role="status">Chargement…</p>}
        {rows?.length === 0 && (
          <p className="empty">
            {review ? "Aucune inscription à traiter." : "Aucun compte correspondant."}
          </p>
        )}
        {!!rows?.length && (
          <div className="scroll">
            <table>
              <thead>
                <tr>
                  <th>Adresse institutionnelle</th>
                  <th>Profil</th>
                  <th>Statut</th>
                  <th>Décision</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.email}</td>
                    <td>{roles[row.role]}</td>
                    <td>
                      <span className={"status " + row.status}>{statuses[row.status]}</span>
                    </td>
                    <td>
                      <div className="action-group">
                        {review ? (
                          <>
                            <button disabled={saving} onClick={() => choose(row, "ACTIF", "Approuver")}>
                              Approuver
                            </button>
                            <button disabled={saving} onClick={() => choose(row, "REFUSE", "Refuser")}>
                              Refuser
                            </button>
                          </>
                        ) : row.id !== session.me.id && ["ACTIF", "BLOQUE"].includes(row.status) ? (
                          <button
                            disabled={saving}
                            onClick={() =>
                              row.status === "ACTIF"
                                ? choose(row, "BLOQUE", "Bloquer")
                                : choose(row, "ACTIF", "Débloquer")
                            }
                          >
                            {row.status === "ACTIF" ? "Bloquer" : "Débloquer"}
                          </button>
                        ) : (
                          "—"
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
