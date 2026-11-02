import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { login as loginRequest, fetchCurrentUser } from '../api/auth.js';

const TOKEN_KEY = 'smartcampus_token';
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_KEY));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(sessionStorage.getItem(TOKEN_KEY)));

  // Au chargement de la page, on vérifie que le token stocké est encore valide.
  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    fetchCurrentUser(token).then((me) => {
      if (cancelled) return;
      if (me && me.success !== false) {
        setUser(me);
      } else {
        sessionStorage.removeItem(TOKEN_KEY);
        setToken(null);
      }
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const login = useCallback(async (email, password) => {
    const result = await loginRequest(email, password);
    if (result.ok && result.data?.success) {
      sessionStorage.setItem(TOKEN_KEY, result.data.token);
      setToken(result.data.token);
      setUser({ email: email.trim().toLowerCase(), role: result.data.role });
    }
    return result;
  }, []);

  // L'API de déconnexion ne gère pas de session : il suffit d'oublier le token côté client.
  const logout = useCallback(() => {
    sessionStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth doit être utilisé dans AuthProvider');
  return context;
}
