import { createContext, useContext, useEffect, useState } from 'react';
import { api, users } from '../api/index.js';
import { session, tokenSession } from './session.js';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [value, setValue] = useState(session.get());
  const [loading, setLoading] = useState(Boolean(session.get()));
  const [error, setError] = useState(null);
  useEffect(() => session.subscribe(setValue), []);
  useEffect(() => {
    if (!session.get()) return;
    const controller = new AbortController();
    users.me(controller.signal).then(user => {
      if (!controller.signal.aborted) session.save({ ...session.get(), user });
    }).catch(problem => {
      if (!controller.signal.aborted) { setError(problem.message); session.save(null); }
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, []);

  async function login(email, password) {
    setError(null);
    const tokens = await users.login({ email: email.trim(), password });
    session.save(tokenSession(tokens));
    try {
      const user = await users.me();
      session.save({ ...session.get(), user });
      return user;
    } catch (problem) { session.save(null); throw problem; }
  }
  async function logout() {
    try { await api.waitForRefresh(); } catch { /* El cierre local no depende del refresh. */ }
    const token = session.get()?.refreshToken;
    session.save(null);
    setError(null);
    if (token) {
      try { await users.logout(token); }
      catch { setError('La sesión local se ha cerrado, pero no se pudo confirmar la revocación en el servidor.'); }
    }
  }
  async function renew() {
    await api.refresh();
    const user = await users.me();
    session.save({ ...session.get(), user });
  }
  return <AuthContext.Provider value={{ user: value?.user || null, loading, error, login, logout, renew }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
