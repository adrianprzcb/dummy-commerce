import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { inventory, store } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';

export default function AdminInventoryPage() {
  const [params, setParams] = useSearchParams();
  const productId = params.get('productId') || '';
  const products = useLoad(store.products);
  const loader = useCallback(async signal => {
    if (!productId) return null;
    try { return await inventory.item(productId, signal); }
    catch (problem) { if (problem.status === 404) return null; throw problem; }
  }, [productId]);
  const stock = useLoad(loader);
  const [quantity, setQuantity] = useState('0');
  const [changeQuantity, setChangeQuantity] = useState('1');
  const [action, setAction] = useState('increase');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null); setNotice(null);
    try {
      if (stock.data) await inventory.change(productId, action, Number(changeQuantity));
      else await inventory.create({ productId, initialQuantity: Number(quantity) });
      stock.reload(); setNotice('Inventario actualizado.');
    } catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  return <>
    <div className="page-heading"><h1>Inventario</h1><button className="secondary" disabled={!productId || busy} onClick={stock.reload}>Consultar stock</button></div>
    <Feedback loading={products.loading || stock.loading} error={products.error || stock.error || error} message={notice} />
    <label className="card">Producto<select value={productId} disabled={busy} onChange={event => { setParams(event.target.value ? { productId: event.target.value } : {}); setError(null); setNotice(null); }}>
      <option value="">Seleccionar producto</option>{products.data?.map(product => <option key={product.id} value={product.id}>{product.name} ({product.status})</option>)}
    </select></label>
    {productId && !stock.loading && !stock.error && <section className="card">
      {stock.data ? <><dl className="stock-summary"><div><dt>Disponible</dt><dd>{stock.data.availableQuantity}</dd></div><div><dt>Reservado</dt><dd>{stock.data.reservedQuantity}</dd></div><div><dt>Total</dt><dd>{stock.data.totalQuantity}</dd></div></dl>
        <form className="inline-form" onSubmit={submit}><label>Operación<select value={action} onChange={event => setAction(event.target.value)}><option value="increase">Aumentar stock</option><option value="decrease">Disminuir stock</option></select></label>
          <label>Cantidad<input type="number" min="1" max="2147483647" step="1" required value={changeQuantity} onChange={event => setChangeQuantity(event.target.value)} /></label><button disabled={busy}>{busy ? 'Guardando…' : 'Aplicar cambio'}</button></form>
      </> : <><p>Este producto todavía no tiene inventario.</p><form className="inline-form" onSubmit={submit}><label>Stock inicial<input type="number" min="0" max="2147483647" step="1" required value={quantity} onChange={event => setQuantity(event.target.value)} /></label><button disabled={busy}>{busy ? 'Guardando…' : 'Crear inventario'}</button></form></>}
    </section>}
  </>;
}
