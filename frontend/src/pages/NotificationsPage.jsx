import { Link } from 'react-router-dom';
import { notifications } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { dateTime } from '../format.js';
import PageHeading from '../components/PageHeading.jsx';
import EmptyState from '../components/EmptyState.jsx';

export default function NotificationsPage() {
  const { data, loading, error, reload } = useLoad(notifications.mine);
  return <>
    <PageHeading eyebrow="Mi cuenta" title="Notificaciones" description="Las novedades de tus pedidos, sin perder detalle."><button className="secondary" onClick={reload}>Actualizar</button></PageHeading>
    <Feedback loading={loading} error={error} />
    {!loading && data?.length === 0 && <EmptyState title="Todo al día"><p>No tienes notificaciones. Pueden tardar unos segundos después de completar un pedido.</p></EmptyState>}
    <div className="notification-list">{data && [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).map(item => <article className="card" key={item.id}>
      <div className="page-heading"><h2>{item.subject}</h2><Status value={item.status} /></div>
      <p className="description">{item.message}</p><p className="muted">{dateTime(item.createdAt)}</p>
      {item.orderId && <Link to={`/orders/${item.orderId}`}>Ver pedido</Link>}
    </article>)}</div>
  </>;
}
