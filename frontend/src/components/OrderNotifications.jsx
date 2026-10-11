import { useEffect, useState } from 'react';
import { notifications } from '../api/index.js';
import Feedback from './Feedback.jsx';
import Status from './Status.jsx';
import { dateTime } from '../format.js';

export default function OrderNotifications({ orderId, orderStatus }) {
  const [result, setResult] = useState({ items: [], loading: true, error: null });
  const [version, setVersion] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let timer;
    let attempts = 0;
    setResult({ items: [], loading: true, error: null });
    async function poll() {
      try {
        const mine = await notifications.mine(controller.signal);
        if (controller.signal.aborted) return;
        const items = mine.filter(item => item.orderId === orderId)
          .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        setResult({ items, loading: false, error: null });
        // Kafka puede generar el aviso después de que el pedido ya haya terminado.
        if ((!items.length || items.some(item => item.status === 'PENDING')) && ++attempts < 30) timer = setTimeout(poll, 2000);
      } catch (error) {
        if (!controller.signal.aborted) setResult(previous => ({ ...previous, loading: false, error }));
      }
    }
    poll();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [orderId, orderStatus, version]);
  return <section className="order-notifications" aria-labelledby="order-notifications-title">
    <div className="page-heading"><h2 id="order-notifications-title">Avisos del pedido</h2><button className="secondary" onClick={() => setVersion(value => value + 1)}>Actualizar avisos</button></div>
    <Feedback loading={result.loading} error={result.error} />
    {!result.loading && !result.error && !result.items.length && <p className="muted">No hay avisos todavía. El aviso de confirmación o cancelación puede tardar unos segundos.</p>}
    <div className="notification-list">{result.items.map(item => {
      const outcome = item.subject === 'Order confirmed' ? 'confirmado' : item.subject === 'Order cancelled' ? 'cancelado' : null;
      return <article className="card" key={item.id}>
        <h3>{outcome ? `Pedido ${outcome}` : item.subject}</h3>
        <p>{outcome ? `Tu pedido se ha ${outcome}.` : item.message}</p>
        <div className="notification-meta"><span className="muted">{dateTime(item.createdAt)}</span><span>Email simulado: <Status value={item.status} /></span></div>
      </article>;
    })}</div>
  </section>;
}
