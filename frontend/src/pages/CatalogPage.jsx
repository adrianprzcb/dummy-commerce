import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { loadCatalog } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import ProductImage from '../components/ProductImage.jsx';
import { money } from '../format.js';

export default function CatalogPage() {
  const { data, loading, error, reload } = useLoad(loadCatalog);
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const location = useLocation();
  const products = data?.products.filter(product => product.status === 'ACTIVE'
    && (!category || product.categoryId === category)
    && product.name.toLowerCase().includes(search.toLowerCase())) || [];
  return <>
    <div className="page-heading"><div><h1>Catálogo</h1><p>Productos disponibles para comprar.</p></div><button className="secondary" onClick={reload}>Actualizar</button></div>
    <Feedback loading={loading} error={error} message={location.state?.notice} />
    <div className="filters">
      <label>Buscar producto<input type="search" value={search} onChange={event => setSearch(event.target.value)} /></label>
      <label>Categoría<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas las categorías</option>{data?.categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    </div>
    {!loading && !error && products.length === 0 && <p>No hay productos disponibles con estos filtros.</p>}
    <div className="product-grid">{products.map(product => <article className="card product-card" key={product.id}>
      <Link to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}><ProductImage product={product} /></Link>
      <div className="card-content"><p className="muted">{data.categories.find(item => item.id === product.categoryId)?.name || 'Sin categoría'}</p>
        <h2><Link to={`/products/${product.id}`}>{product.name}</Link></h2><p className="price">{money(product.price)}</p>
        <Link className="button" to={`/products/${product.id}`}>Ver producto</Link></div>
    </article>)}</div>
  </>;
}
