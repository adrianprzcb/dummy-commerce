import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { loadCatalog } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import ProductImage from '../components/ProductImage.jsx';
import Icon from '../components/Icon.jsx';
import EmptyState from '../components/EmptyState.jsx';
import { money } from '../format.js';
import Status from '../components/Status.jsx';
import { productAvailability } from '../productAvailability.js';

export default function CatalogPage() {
  const { data, loading, error, reload } = useLoad(loadCatalog);
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const location = useLocation();
  const products = data?.products.filter(product => product.status === 'ACTIVE'
    && (!category || product.categoryId === category)
    && product.name.toLowerCase().includes(search.toLowerCase())) || [];
  return <>
    <section className="catalog-hero"><div className="hero-copy"><p className="eyebrow">Bienvenido a Dummy Commerce</p><h1>Tu próxima compra<br />empieza aquí.</h1><p>Encuentra esos pequeños detalles que hacen tu día a día un poco más tuyo.</p></div></section>
    <Feedback loading={loading} error={error} message={location.state?.notice} />
    {data?.availabilityError && <p className="notice" role="status">No se ha podido consultar el stock. Pulsa Actualizar para volver a intentarlo.</p>}
    <div className="page-heading catalog-heading" id="catalog"><div><p className="eyebrow">La tienda</p><h2>Catálogo</h2></div><button className="secondary" onClick={reload}>Actualizar</button></div>
    <div className="filters card">
      <label>Buscar producto<span className="search-field"><Icon name="search" /><input type="search" placeholder="¿Qué estás buscando?" value={search} onChange={event => setSearch(event.target.value)} /></span></label>
      <label>Categoría<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas las categorías</option>{data?.categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    </div>
    {!loading && !error && <p className="catalog-count">{products.length} {products.length === 1 ? 'producto' : 'productos'}</p>}
    {!loading && !error && products.length === 0 && <EmptyState title="No hay productos"><p>Prueba otra búsqueda o categoría. Si el catálogo está vacío, el administrador puede añadir productos desde su panel.</p></EmptyState>}
    <div className="product-grid">{products.map(product => <article className="card product-card" key={product.id}>
      <Link to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}><ProductImage product={product} /></Link>
      <div className="card-content"><p className="product-category">{data.categories.find(item => item.id === product.categoryId)?.name || 'Sin categoría'}</p>
        <h2><Link to={`/products/${product.id}`}>{product.name}</Link></h2><p className="price">{money(product.price)}</p>
        <Status value={productAvailability(product)} />
        <Link className="product-link" to={`/products/${product.id}`}>Ver producto <Icon name="arrow" /></Link></div>
    </article>)}</div>
  </>;
}
