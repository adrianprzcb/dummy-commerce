import { useCallback, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { payments } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { dateTime } from '../format.js';
import PageHeading from '../components/PageHeading.jsx';

export default function AdminPaymentsPage() {
  const [params, setParams] = useSearchParams();
  const orderId = params.get('orderId') || '';
  const [input, setInput] = useState(orderId);
  const loader = useCallback(signal => orderId ? payments.forOrder(orderId, signal) : Promise.resolve(null), [orderId]);
  const { data, loading, error, reload } = useLoad(loader);
  function submit(event) {
    event.preventDefault();
    if (orderId === input.trim()) reload();
    else setParams({ orderId: input.trim() });
  }
  return <>
    <PageHeading title="Consulta de pagos" description="Comprueba el resultado del pago simulado de un pedido." />
    <form className="card inline-form" onSubmit={submit}><label>ID del pedido<input required pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" value={input} onChange={event => setInput(event.target.value)} /></label><button disabled={loading}>Consultar pago</button></form>
    <Feedback loading={loading} error={error} />
    {data && <section className="card"><h2>Pago <span className="id">{data.id}</span></h2><Status value={data.status} /><dl>
      <dt>Pedido</dt><dd className="id">{data.orderId}</dd><dt>Importe</dt><dd>{new Intl.NumberFormat('es-ES', { style: 'currency', currency: data.currency }).format(data.amount)}</dd>
      <dt>Actualizado</dt><dd>{dateTime(data.updatedAt)}</dd></dl></section>}
  </>;
}
