import { Link } from 'react-router-dom';
import { orders } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { dateTime, money } from '../format.js';

export default function OrdersPage() {
  const { data, loading, error, reload } = useLoad(orders.mine);
  return <>
    <div className="page-heading"><h1>Mis pedidos</h1><button className="secondary" onClick={reload}>Actualizar</button></div>
    <Feedback loading={loading} error={error} />
    {!loading && data?.length === 0 && <p>Aún no tienes pedidos. <Link to="/">Ver catálogo</Link></p>}
    {data?.length > 0 && <div className="table-wrap"><table><thead><tr><th>Pedido</th><th>Fecha</th><th>Total</th><th>Estado</th></tr></thead>
      <tbody>{[...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).map(order => <tr key={order.id}>
        <td><Link className="id" to={`/orders/${order.id}`}>{order.id}</Link></td><td>{dateTime(order.createdAt)}</td><td>{money(order.totalAmount)}</td><td><Status value={order.status} /></td>
      </tr>)}</tbody></table></div>}
  </>;
}
