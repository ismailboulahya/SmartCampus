import { createRoot } from "react-dom/client";
import { useEffect, useState } from "react";
import Registration from "./Registration";
import SessionPage from "./SessionPage";
import "./style.css";

function readConfirmationToken() {
  const token = window.location.hash.slice(1);
  if (token) history.replaceState(null, "", location.pathname + location.search);
  return token;
}

function App() {
  const admin = location.pathname === "/admin";
  const login = location.pathname === "/connexion";
  const [emailToken, setEmailToken] = useState(readConfirmationToken);
  useEffect(() => {
    document.title = (admin ? "Administration" : login ? "Connexion" : "Inscription") + " · SmartCampus";
    const onLink = () => setEmailToken(readConfirmationToken());
    window.addEventListener("hashchange", onLink);
    return () => window.removeEventListener("hashchange", onLink);
  }, [admin, login]);
  return (
    <>
      <header className="top">
        <div className="top-inner">
          <a className="brand" href="/">
            <span className="mark" aria-hidden="true">
              S
            </span>
            SmartCampus
          </a>
          <nav aria-label="Navigation principale">
            <a href="/inscription" aria-current={!admin && !login ? "page" : undefined}>
              Inscription
            </a>
            <a href="/connexion" aria-current={login ? "page" : undefined}>
              Connexion
            </a>
            <a href="/admin" aria-current={admin ? "page" : undefined}>
              Administration
            </a>
          </nav>
        </div>
      </header>
      <main>
        {admin || login ? (
          <SessionPage admin={admin} />
        ) : (
          <Registration key={emailToken} initialToken={emailToken} />
        )}
      </main>
    </>
  );
}

createRoot(document.getElementById("root")).render(<App />);
