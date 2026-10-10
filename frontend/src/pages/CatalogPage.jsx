import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { loadCatalog } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import ProductImage from '../components/ProductImage.jsx';
import Icon from '../components/Icon.jsx';
import EmptyState from '../components/EmptyState.jsx';
import { money } from '../format.js';

export default function CatalogPage() {
  const { data, loading, error, reload } = useLoad(loadCatalog);
  const [category, setCategory] = useState('');
  const [search, setSearch] = useState('');
  const location = useLocation();
  const featured = data?.products.find(product => product.status === 'ACTIVE');
  const products = data?.products.filter(product => product.status === 'ACTIVE'
    && (!category || product.categoryId === category)
    && product.name.toLowerCase().includes(search.toLowerCase())) || [];
  return <>
    <section className="catalog-hero"><div className="hero-copy"><p className="eyebrow">Bienvenido a Dummy Commerce</p><h1>Tu próxima compra<br />empieza aquí.</h1><p>Encuentra esos pequeños detalles que hacen tu día a día un poco más tuyo.</p><a className="button" href="#catalog">Explorar productos <Icon name="arrow" /></a></div>
      {featured ? <Link className="hero-featured" to={`/products/${featured.id}`} aria-label={`Descubrir ${featured.name}`}><p className="eyebrow">En el escaparate</p><ProductImage product={featured} /><div className="featured-caption"><div><h2>{featured.name}</h2><p>{money(featured.price)}</p></div><span className="featured-arrow"><Icon name="arrow" /></span></div></Link>
        : <div className="hero-brand-panel"><Icon name="bag" /><p className="eyebrow">Dummy Commerce</p><h2>Pequeños detalles.<br />Grandes favoritos.</h2></div>}
    </section>
    <Feedback loading={loading} error={error} message={location.state?.notice} />
    <div className="page-heading catalog-heading" id="catalog"><div><p className="eyebrow">La tienda</p><h2>Catálogo</h2><p>Pequeños detalles para tu día a día.</p></div><button className="secondary" onClick={reload}>Actualizar</button></div>
    <div className="filters card">
      <label>Buscar producto<span className="search-field"><Icon name="search" /><input type="search" placeholder="¿Qué estás buscando?" value={search} onChange={event => setSearch(event.target.value)} /></span></label>
      <label>Categoría<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Todas las categorías</option>{data?.categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    </div>
    {!loading && !error && <p className="catalog-count">{products.length} {products.length === 1 ? 'producto disponible' : 'productos disponibles'}</p>}
    {!loading && !error && products.length === 0 && <EmptyState title="No hay productos disponibles"><p>Prueba otra búsqueda o categoría. Si el catálogo está vacío, el administrador puede añadir productos desde su panel.</p></EmptyState>}
    <div className="product-grid">{products.map(product => <article className="card product-card" key={product.id}>
      <Link to={`/products/${product.id}`} aria-label={`Ver ${product.name}`}><ProductImage product={product} /></Link>
      <div className="card-content"><p className="product-category">{data.categories.find(item => item.id === product.categoryId)?.name || 'Sin categoría'}</p>
        <h2><Link to={`/products/${product.id}`}>{product.name}</Link></h2><p className="price">{money(product.price)}</p>
        <Link className="product-link" to={`/products/${product.id}`}>Ver producto <Icon name="arrow" /></Link></div>
    </article>)}</div>
  </>;
}
