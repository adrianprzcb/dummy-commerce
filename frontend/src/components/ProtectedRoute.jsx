import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';

export default function ProtectedRoute({ admin = false }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return <p role="status">Comprobando sesión…</p>;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  if (admin && user.role !== 'ADMIN') return <Navigate to="/" state={{ notice: 'Esta página requiere permisos de administrador.' }} replace />;
  return <Outlet />;
}
