let current = null;
const listeners = new Set();
const key = 'dummy-commerce-session';

try {
  const stored = JSON.parse(sessionStorage.getItem(key));
  if (stored?.accessToken && stored?.refreshToken) current = stored;
} catch { /* El navegador puede deshabilitar el almacenamiento. */ }

export const session = {
  get: () => current,
  save(value) {
    current = value;
    try {
      if (value) sessionStorage.setItem(key, JSON.stringify(value));
      else sessionStorage.removeItem(key);
    } catch { /* La sesión sigue funcionando en memoria. */ }
    listeners.forEach(listener => listener(current));
  },
  subscribe(listener) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};

export function tokenSession(tokens, user = null) {
  return { ...tokens, user, expiresAt: Date.now() + tokens.accessTokenExpiresIn * 1000 };
}
