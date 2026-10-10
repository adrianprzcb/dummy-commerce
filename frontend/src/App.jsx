import { useState } from 'react';
import { Link, NavLink, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext.jsx';
import { useCart } from './components/CartContext.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Feedback from './components/Feedback.jsx';
import Icon from './components/Icon.jsx';
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
  const count = items.reduce((sum, item) => sum + item.quantity, 0);
  async function sessionAction(action) {
    setBusy(true); setMessage(null); setProblem(null);
    try { await action(); if (action === renew) setMessage('Sesión renovada.'); }
    catch (failure) { setProblem(failure); }
    finally { setBusy(false); }
  }
  return <>
    <a className="skip-link" href="#main">Ir al contenido</a>
    <div className="demo-bar">Una tienda de demostración. Los pagos y las notificaciones son simulados.</div>
    <header className="site-header"><div className="header-inner"><Link className="brand" to="/" aria-label="Dummy Commerce · Ir al catálogo"><span className="brand-mark"><Icon name="bag" /></span><span>dummy<span className="brand-subtitle">commerce</span></span></Link>
      <nav className="main-nav" aria-label="Navegación principal"><NavLink end to="/">Catálogo</NavLink>
        {user && <><NavLink to="/orders">Mis pedidos</NavLink><NavLink to="/notifications">Notificaciones</NavLink>{user.role === 'ADMIN' && <NavLink to="/admin">Administración</NavLink>}</>}
      </nav>
      <div className="session-actions"><NavLink className="cart-link" to="/cart" aria-label={`Carrito (${count})`}><Icon name="bag" /><span>Carrito</span><span className="cart-count">{count}</span></NavLink>
        {loading ? <span>Comprobando sesión…</span> : user ? <div className="account-actions"><span className="account-email">{user.email}<span className="account-role">{user.role === 'ADMIN' ? 'Administrador' : 'Mi cuenta'}</span></span><button className="text-button" disabled={busy} onClick={() => sessionAction(renew)}>Renovar sesión</button><button className="secondary" disabled={busy} onClick={() => sessionAction(logout)}>Salir</button></div> : <><Link className="login-link" to="/login">Iniciar sesión</Link><Link className="button" to="/register">Crear cuenta</Link></>}
      </div>
    </div></header>
    <main id="main"><Feedback error={error || problem} message={message} /><Outlet /></main>
    <footer className="site-footer"><div><Link className="footer-brand" to="/">Dummy Commerce<span>Una experiencia de compra completa.</span></Link><p>Proyecto de portfolio · Demo con pagos simulados</p><Link to="/">Explorar catálogo <Icon name="arrow" /></Link></div></footer>
  </>;
}
function AdminLayout() {
  return <div className="admin-layout"><aside className="admin-sidebar"><p className="eyebrow">Panel de gestión</p><h2>Administración</h2><p className="muted">Tu tienda, en orden.</p><nav className="admin-nav" aria-label="Administración"><NavLink to="/admin/products">Productos<span>Catálogo e imágenes</span></NavLink><NavLink to="/admin/categories">Categorías<span>Organización del catálogo</span></NavLink><NavLink to="/admin/inventory">Inventario<span>Stock y disponibilidad</span></NavLink><NavLink to="/admin/payments">Pagos<span>Consulta por pedido</span></NavLink></nav><Link className="back-link" to="/">Volver a la tienda</Link></aside><div className="admin-content"><Outlet /></div></div>;
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
