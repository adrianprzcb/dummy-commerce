import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { orders, store } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import { useAuth } from '../auth/AuthContext.jsx';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { dateTime, money } from '../format.js';

export default function OrderPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState(null);
  const [polling, setPolling] = useState(true);
  const [version, setVersion] = useState(0);
  const catalog = useLoad(store.products);
  useEffect(() => {
    const controller = new AbortController();
    let timer;
    let attempts = 0;
    setOrder(null); setError(null); setPolling(true);
    async function poll() {
      try {
        const current = await orders.order(id, controller.signal);
        if (controller.signal.aborted) return;
        setOrder(current);
        if (!['CONFIRMED', 'CANCELLED'].includes(current.status) && ++attempts < 30) timer = setTimeout(poll, 2000);
        else setPolling(false);
      } catch (problem) {
        if (!controller.signal.aborted) { setError(problem); setPolling(false); }
      }
    }
    poll();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [id, version]);
  return <>
    <Link to="/orders">Volver a mis pedidos</Link>
    <div className="page-heading"><h1>Detalle del pedido</h1><button className="secondary" onClick={() => setVersion(value => value + 1)}>Actualizar estado</button></div>
    <Feedback loading={!order && polling} error={error} />
    {order && <>
      <section className="card"><p className="id">{order.id}</p><p>{dateTime(order.createdAt)}</p><Status value={order.status} />
        {polling && <p role="status">Estamos procesando tu pedido. El estado se actualiza automáticamente.</p>}
        {!polling && !['CONFIRMED', 'CANCELLED'].includes(order.status) && <p className="notice">El pedido sigue en proceso. Puedes volver a consultar su estado más tarde.</p>}
        {order.status === 'CONFIRMED' && <p className="notice success">Tu pedido se ha confirmado.</p>}
        {order.status === 'CANCELLED' && <p className="notice error" role="alert">Pedido cancelado. Puede deberse a stock insuficiente, un pago rechazado o una compensación. Consulta tus notificaciones.</p>}
      </section>
      <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Cantidad</th><th>Precio de compra</th><th>Subtotal</th></tr></thead><tbody>
        {order.items.map(item => <tr key={item.id}><td><Link to={`/products/${item.productId}`}>{catalog.data?.find(product => product.id === item.productId)?.name || item.productId}</Link></td>
          <td>{item.quantity}</td><td>{money(item.unitPrice)}</td><td>{money(item.subtotal)}</td></tr>)}
      </tbody></table></div><p className="price">Total: {money(order.totalAmount)}</p>
      <div className="actions"><Link to="/notifications">Ver notificaciones</Link>{user.role === 'ADMIN' && <Link to={`/admin/payments?orderId=${order.id}`}>Consultar pago</Link>}</div>
    </>}
  </>;
}
