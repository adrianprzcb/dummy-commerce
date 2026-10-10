import { useState } from 'react';
import { store } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import PageHeading from '../components/PageHeading.jsx';
import EmptyState from '../components/EmptyState.jsx';

export default function AdminCategoriesPage() {
  const { data, loading, error, reload } = useLoad(store.categories);
  const [name, setName] = useState('');
  const [busy, setBusy] = useState(false);
  const [problem, setProblem] = useState(null);
  const [notice, setNotice] = useState(null);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setProblem(null); setNotice(null);
    try { await store.createCategory({ name: name.trim() }); setName(''); setNotice('Categoría creada.'); reload(); }
    catch (failure) { setProblem(failure); }
    finally { setBusy(false); }
  }
  return <>
    <PageHeading title="Categorías" description="Organiza los productos para que sean más fáciles de encontrar." /><Feedback loading={loading} error={error || problem} message={notice} />
    <form className="card inline-form" onSubmit={submit}><label>Nombre de categoría<input required maxLength={100} value={name} onChange={event => setName(event.target.value)} /></label><button disabled={busy}>{busy ? 'Guardando…' : 'Crear categoría'}</button></form>
    {data?.length > 0 && <div className="table-wrap"><table><thead><tr><th>Nombre</th><th>ID</th></tr></thead><tbody>{data.map(item => <tr key={item.id}><td>{item.name}</td><td className="id">{item.id}</td></tr>)}</tbody></table></div>}
    {!loading && data?.length === 0 && <EmptyState title="Todavía no hay categorías"><p>Añade la primera desde el formulario de arriba.</p></EmptyState>}
  </>;
}
