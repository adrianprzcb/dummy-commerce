import { Link } from 'react-router-dom';
import { loadCatalog } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import Status from '../components/Status.jsx';
import { money } from '../format.js';

export default function AdminProductsPage() {
  const { data, loading, error, reload } = useLoad(loadCatalog);
  return <>
    <div className="page-heading"><h1>Productos</h1><div className="actions"><button className="secondary" onClick={reload}>Actualizar</button><Link className="button" to="/admin/products/new">Nuevo producto</Link></div></div>
    <Feedback loading={loading} error={error} />
    {!loading && data?.products.length === 0 && <p>Todavía no hay productos. Crea primero una categoría.</p>}
    {data && <div className="table-wrap"><table><thead><tr><th>Producto</th><th>Categoría</th><th>Precio</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>{data.products.map(product => <tr key={product.id}>
      <td>{product.name}</td><td>{data.categories.find(item => item.id === product.categoryId)?.name}</td><td>{money(product.price)}</td><td><Status value={product.status} /></td>
      <td><div className="actions"><Link to={`/admin/products/${product.id}`}>Editar e imágenes</Link><Link to={`/admin/inventory?productId=${product.id}`}>Stock</Link></div></td>
    </tr>)}</tbody></table></div>}
  </>;
}
