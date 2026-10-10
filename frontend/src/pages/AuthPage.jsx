import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { users } from '../api/index.js';
import Feedback from '../components/Feedback.jsx';

export default function AuthPage({ register = false }) {
  const { user, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [email, setEmail] = useState(location.state?.email || '');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  if (user) return <Navigate to="/" replace />;

  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    setError(null);
    if (register && password !== confirmation) { setError('Las contraseñas no coinciden.'); return; }
    setBusy(true);
    try {
      if (register) {
        await users.register({ email: email.trim(), password });
        navigate('/login', { replace: true, state: { email, notice: 'Cuenta creada. Ya puedes iniciar sesión.' } });
      } else {
        await login(email, password);
        const next = location.state?.from;
        navigate(typeof next === 'string' && next.startsWith('/') && !next.startsWith('//') ? next : '/', { replace: true });
      }
    } catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  return <div className="auth-layout"><section className="auth-intro"><p className="eyebrow">Tu espacio en la tienda</p><h2>{register ? 'Todo empieza con tu cuenta.' : 'Qué bueno tenerte de vuelta.'}</h2><p>Guarda tus compras en un solo lugar y sigue cada pedido hasta su confirmación.</p><ul><li>Compra los productos de tu carrito.</li><li>Consulta el estado de tus pedidos.</li><li>Recibe las novedades de tus compras.</li></ul><Link className="back-link" to="/">Seguir explorando el catálogo →</Link></section><section className="card auth-card">
    <p className="eyebrow">{register ? 'Un paso más' : 'Bienvenido de nuevo'}</p><h1>{register ? 'Crear cuenta' : 'Iniciar sesión'}</h1>
    <p className="muted">{register ? 'Crea tu cuenta para completar tu primera compra.' : 'Accede con tu email y contraseña.'}</p>
    <Feedback error={error} message={location.state?.notice} />
    <form onSubmit={submit}>
      <label>Email<input type="email" name="email" autoComplete="email" required maxLength={320} value={email} onChange={event => setEmail(event.target.value)} /></label>
      <label>Contraseña<input type="password" name="password" autoComplete={register ? 'new-password' : 'current-password'} required minLength={register ? 8 : undefined} maxLength={register ? 72 : undefined} value={password} onChange={event => setPassword(event.target.value)} /></label>
      {register && <label>Repetir contraseña<input type="password" name="confirmation" autoComplete="new-password" required minLength={8} maxLength={72} value={confirmation} onChange={event => setConfirmation(event.target.value)} /></label>}
      <button disabled={busy}>{busy ? 'Enviando…' : register ? 'Crear cuenta' : 'Entrar'}</button>
    </form>
    <p className="auth-switch">{register ? '¿Ya tienes cuenta? ' : '¿Todavía no tienes cuenta? '}<Link to={register ? '/login' : '/register'}>{register ? 'Inicia sesión' : 'Regístrate'}</Link></p>
    <p className="auth-note">Puedes consultar el catálogo y preparar tu carrito sin una cuenta.</p>
  </section></div>;
}
