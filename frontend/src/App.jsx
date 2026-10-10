import { useState } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import { useCart } from './components/CartContext.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Feedback from './components/Feedback.jsx';
import AuthPage from './pages/AuthPage.jsx';
import CatalogPage from './pages/CatalogPage.jsx';
import ProductPage from './pages/ProductPage.jsx';
import CartPage from './pages/CartPage.jsx';
import OrdersPage from './pages/OrdersPage.jsx';
import OrderPage from './pages/OrderPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import AdminProductsPage from './pages/AdminProductsPage.jsx';
import AdminProductPage from './pages/AdminProductPage.jsx';
import AdminCategoriesPage from './pages/AdminCategoriesPage.jsx';
import AdminInventoryPage from './pages/AdminInventoryPage.jsx';
import AdminPaymentsPage from './pages/AdminPaymentsPage.jsx';

function Layout() {
  const { user, loading, error, logout, renew } = useAuth();
  const { items } = useCart();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [problem, setProblem] = useState(null);
  async function sessionAction(action) {
    setBusy(true); setMessage(null); setProblem(null);
    try { await action(); if (action === renew) setMessage('Sesión renovada.'); }
    catch (failure) { setProblem(failure); }
    finally { setBusy(false); }
  }
  return <>
    <a className="skip-link" href="#main">Ir al contenido</a>
    <header><div className="header-inner"><Link className="brand" to="/">Dummy Commerce</Link>
      <nav aria-label="Navegación principal"><NavLink end to="/">Catálogo</NavLink><NavLink to="/cart">Carrito ({items.reduce((sum, item) => sum + item.quantity, 0)})</NavLink>
        {user && <><NavLink to="/orders">Mis pedidos</NavLink><NavLink to="/notifications">Notificaciones</NavLink>{user.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}</>}
      </nav>
      <div className="session-actions">{loading ? <span>Comprobando sesión…</span> : user ? <><span className="account-email">{user.email}</span><button className="text-button" disabled={busy} onClick={() => sessionAction(renew)}>Renovar sesión</button><button className="secondary" disabled={busy} onClick={() => sessionAction(logout)}>Salir</button></> : <Link className="button" to="/login">Iniciar sesión</Link>}</div>
    </div></header>
    <main id="main"><Feedback error={error || problem} message={message} /><Outlet /></main>
    <footer>Dummy Commerce</footer>
  </>;
}
function AdminLayout() {
  return <><nav className="admin-nav" aria-label="Administración"><NavLink to="/admin/products">Productos</NavLink><NavLink to="/admin/categories">Categorías</NavLink><NavLink to="/admin/inventory">Inventario</NavLink><NavLink to="/admin/payments">Pagos</NavLink></nav><Outlet /></>;
}
export default function App() {
  return <Routes><Route element={<Layout />}>
    <Route index element={<CatalogPage />} />
    <Route path="login" element={<AuthPage key="login" />} /><Route path="register" element={<AuthPage key="register" register />} />
    <Route path="products/:id" element={<ProductPage />} /><Route path="cart" element={<CartPage />} />
    <Route element={<ProtectedRoute />}><Route path="orders" element={<OrdersPage />} /><Route path="orders/:id" element={<OrderPage />} /><Route path="notifications" element={<NotificationsPage />} /></Route>
    <Route element={<ProtectedRoute admin />}><Route path="admin" element={<AdminLayout />}>
      <Route index element={<Navigate to="products" replace />} /><Route path="products" element={<AdminProductsPage />} />
      <Route path="products/new" element={<AdminProductPage />} /><Route path="products/:id" element={<AdminProductPage />} />
      <Route path="categories" element={<AdminCategoriesPage />} /><Route path="inventory" element={<AdminInventoryPage />} /><Route path="payments" element={<AdminPaymentsPage />} />
    </Route></Route>
    <Route path="*" element={<section className="card"><h1>Página no encontrada</h1><Link to="/">Volver al catálogo</Link></section>} />
  </Route></Routes>;
}
