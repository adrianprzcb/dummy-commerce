import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../components/CartContext.jsx';
import { useAuth } from '../auth/AuthContext.jsx';
import { orders, store } from '../api/index.js';
import Feedback from '../components/Feedback.jsx';
import { cartTotal, money } from '../format.js';

export default function CartPage() {
  const cart = useCart();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const submitting = useRef(false);
  async function checkout() {
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true); setError(null); setNotice(null);
    try {
      const order = await orders.create(cart.items.map(({ productId, quantity, unitPrice }) => ({ productId, quantity, unitPrice })));
      cart.clear();
      navigate(`/orders/${order.id}`, { state: { created: true } });
    } catch (problem) { setError(problem); }
    finally { setBusy(false); submitting.current = false; }
  }
  async function updatePrices() {
    setBusy(true); setError(null); setNotice(null);
    try {
      const updated = await Promise.all(cart.items.map(async item => {
        try {
          const product = await store.product(item.productId);
          return { ...item, name: product.name, unitPrice: product.price, status: product.status };
        } catch (problem) {
          if (problem.status === 404) return { ...item, status: 'INACTIVE' };
          throw problem;
        }
      }));
      cart.replace(updated);
      setNotice('Carrito actualizado. Revisa los precios y elimina los productos no disponibles.');
    } catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  return <>
    <h1>Carrito</h1><Feedback error={error} message={notice} />
    {cart.items.length === 0 ? <p>Tu carrito está vacío. <Link to="/">Ver catálogo</Link></p> : <>
      <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Cantidad</th><th>Precio</th><th>Subtotal</th><th>Acciones</th></tr></thead>
        <tbody>{cart.items.map(item => <tr key={item.productId}>
          <td><Link to={`/products/${item.productId}`}>{item.name}</Link>{item.status !== 'ACTIVE' && <p className="error-text">No disponible</p>}</td>
          <td><input aria-label={`Cantidad de ${item.name}`} className="quantity" type="number" min="1" max="999" step="1" required disabled={busy} value={item.quantity} onChange={event => cart.quantity(item.productId, Number(event.target.value))} /></td>
          <td>{money(item.unitPrice)}</td><td>{money(cartTotal([item]))}</td>
          <td><button className="secondary" disabled={busy} onClick={() => cart.remove(item.productId)}>Quitar</button></td>
        </tr>)}</tbody></table></div>
      <div className="checkout-summary"><p className="price">Total: {money(cartTotal(cart.items))}</p>
        <div className="actions"><button className="secondary" disabled={busy} onClick={updatePrices}>Actualizar carrito</button>
          {user ? <button disabled={busy || cart.items.some(item => item.status !== 'ACTIVE') || cartTotal(cart.items) <= 0} onClick={checkout}>{busy ? 'Procesando…' : 'Confirmar compra'}</button>
            : <Link className="button" to="/login" state={{ from: '/cart' }}>Inicia sesión para comprar</Link>}
        </div>
        <p className="muted">El importe definitivo se valida al crear el pedido.</p>
        {cartTotal(cart.items) <= 0 && <p className="notice">El pedido debe tener un importe mayor que cero.</p>}
      </div>
    </>}
  </>;
}
