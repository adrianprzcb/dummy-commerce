import { Link } from 'react-router-dom';
import { loadCatalog } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { money } from '../format.js';
import PageHeading from '../components/PageHeading.jsx';
import EmptyState from '../components/EmptyState.jsx';

export default function AdminProductsPage() {
  const { data, loading, error, reload } = useLoad(loadCatalog);
  return <>
    <PageHeading title="Productos" description="Gestiona el catálogo, sus imágenes y disponibilidad."><button className="secondary" onClick={reload}>Actualizar</button><Link className="button" to="/admin/products/new">Nuevo producto</Link></PageHeading>
    <Feedback loading={loading} error={error} />
    {!loading && data?.products.length === 0 && <EmptyState title="Tu catálogo empieza aquí"><p>Crea primero una categoría y después añade tu primer producto.</p><Link className="button" to="/admin/categories">Ir a categorías</Link></EmptyState>}
    {data?.products.length > 0 && <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Categoría</th><th>Precio</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{data.products.map(product => <tr key={product.id}>
      <td>{product.name}</td><td>{data.categories.find(item => item.id === product.categoryId)?.name}</td><td>{money(product.price)}</td><td><Status value={product.status} /></td>
      <td><div className="actions"><Link to={`/admin/products/${product.id}`}>Editar e imágenes</Link><Link to={`/admin/inventory?productId=${product.id}`}>Stock</Link></div></td>
    </tr>)}</tbody></table></div>}
  </>;
}
