import { useCallback, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { store } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import { useCart } from '../components/CartContext.jsx';
import Feedback from '../components/Feedback.jsx';
import ProductImage from '../components/ProductImage.jsx';
import Status from '../components/Status.jsx';
import { money } from '../format.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function ProductPage() {
  const { id } = useParams();
  const loader = useCallback(signal => store.product(id, signal), [id]);
  const { data: product, loading, error } = useLoad(loader);
  const { add } = useCart();
  const { user } = useAuth();
  const [quantity, setQuantity] = useState('1');
  const [notice, setNotice] = useState(null);
  const [cartError, setCartError] = useState(null);
  function addItem(event) {
    event.preventDefault();
    try { add(product, Number(quantity)); setCartError(null); setNotice('Producto añadido al carrito.'); }
    catch (problem) { setCartError(problem); }
  }
  return <>
    <Link className="back-link" to="/">← Volver al catálogo</Link><Feedback loading={loading} error={error || cartError} message={notice} />
    {product && !loading && <section className="card product-detail">
      <div><ProductImage product={product} className="large" />
        {product.images.length > 1 && <div className="image-list">{product.images.map(image => <ProductImage key={image.id} product={product} image={image} />)}</div>}
      </div>
      <div className="product-info"><p className="eyebrow">Detalle del producto</p><h1>{product.name}</h1><p className="price">{money(product.price)}</p><Status value={product.status} /><p className="description">{product.description || 'Sin descripción.'}</p>
        {product.status === 'ACTIVE' ? <form className="purchase-form" onSubmit={addItem}>
          <label>Cantidad<input type="number" min="1" max="999" step="1" required value={quantity} onChange={event => setQuantity(event.target.value)} /></label>
          <div className="actions"><button>Añadir al carrito</button><Link to="/cart">Ver carrito</Link></div>
        </form> : <p className="notice">Este producto no está disponible para comprar.</p>}
        {!user && <p className="muted purchase-note">Añádelo al carrito ahora. Solo necesitas iniciar sesión al confirmar la compra.</p>}
      </div>
    </section>}
  </>;
}
