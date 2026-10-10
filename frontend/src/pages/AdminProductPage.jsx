import { useCallback, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { store, uploadImageBytes } from '../api/index.js';
import { useLoad } from '../api/useLoad.js';
import Feedback from '../components/Feedback.jsx';
import ProductImage from '../components/ProductImage.jsx';
import Status from '../components/Status.jsx';

export default function AdminProductPage() {
  const { id } = useParams();
  const loader = useCallback(async signal => {
    const [product, categories] = await Promise.all([id ? store.product(id, signal) : Promise.resolve(null), store.categories(signal)]);
    return { product, categories };
  }, [id]);
  const { data, loading, error, reload } = useLoad(loader);
  return <>
    <Link to="/admin/products">Volver a productos</Link><h1>{id ? 'Editar producto' : 'Nuevo producto'}</h1>
    <Feedback loading={loading} error={error} />
    {data && !loading && <>
      <ProductForm key={id || 'new'} product={data.product} categories={data.categories} reload={reload} />
      {data.product && <ProductImages product={data.product} reload={reload} />}
    </>}
  </>;
}

function ProductForm({ product, categories, reload }) {
  const navigate = useNavigate();
  const [name, setName] = useState(product?.name || '');
  const [description, setDescription] = useState(product?.description || '');
  const [price, setPrice] = useState(product?.price ?? '');
  const [categoryId, setCategoryId] = useState(product?.categoryId || '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(null); setNotice(null);
    try {
      const body = { name: name.trim(), description: description.trim(), price: Number(price), categoryId };
      const saved = product ? await store.updateProduct(product.id, body) : await store.createProduct(body);
      if (!product) navigate(`/admin/products/${saved.id}`, { replace: true });
      else setNotice('Producto guardado.');
    } catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  async function changeStatus(action) {
    setBusy(true); setError(null);
    try { await store.changeStatus(product.id, action); reload(); }
    catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  return <section className="card">
    <Feedback error={error} message={notice} />
    {categories.length === 0 && <p className="notice">Necesitas <Link to="/admin/categories">crear una categoría</Link> antes de guardar un producto.</p>}
    <form onSubmit={submit}>
      <label>Nombre<input required maxLength={200} value={name} onChange={event => setName(event.target.value)} /></label>
      <label>Descripción<textarea maxLength={2000} rows={4} value={description} onChange={event => setDescription(event.target.value)} /></label>
      <div className="form-row"><label>Precio (€)<input type="number" required min="0" step="0.01" value={price} onChange={event => setPrice(event.target.value)} /></label>
        <label>Categoría<select required value={categoryId} onChange={event => setCategoryId(event.target.value)}><option value="">Seleccionar categoría</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>
      <button disabled={busy || categories.length === 0}>{busy ? 'Guardando…' : 'Guardar producto'}</button>
    </form>
    {product && <div className="product-state"><h2>Estado</h2><Status value={product.status} />
      <div className="actions"><button className="secondary" disabled={busy || ['ACTIVE', 'DISCONTINUED'].includes(product.status)} onClick={() => changeStatus('activate')}>Activar</button>
        <button className="secondary" disabled={busy || ['INACTIVE', 'DISCONTINUED'].includes(product.status)} onClick={() => changeStatus('deactivate')}>Desactivar</button>
        <button className="danger" disabled={busy || product.status === 'DISCONTINUED'} onClick={() => changeStatus('discontinue')}>Retirar definitivamente</button></div>
      <p className="muted">Un producto retirado no se puede volver a activar.</p>
      <Link to={`/admin/inventory?productId=${product.id}`}>Gestionar stock</Link>
    </div>}
  </section>;
}

function ProductImages({ product, reload }) {
  const [file, setFile] = useState(null);
  const [altText, setAltText] = useState('');
  const [primary, setPrimary] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [stage, setStage] = useState(null);
  const [pending, setPending] = useState(null);
  const fileInput = useRef(null);
  async function finalize(upload) {
    setStage('Confirmando imagen…');
    await store.confirmImage(product.id, upload.imageId, upload.metadata);
    setPending(null); setFile(null); setAltText('');
    if (fileInput.current) fileInput.current.value = '';
    reload();
  }
  async function upload(event) {
    event.preventDefault();
    if (!file) { setError('Selecciona una imagen.'); return; }
    if (!file.type.startsWith('image/') || file.size > 10 * 1024 * 1024) { setError('Selecciona una imagen de hasta 10 MB.'); return; }
    setBusy(true); setError(null); setStage('Preparando subida…');
    try {
      const prepared = await store.prepareImage(product.id);
      const metadata = { altText: altText.trim() || product.name,
        position: product.images.length ? Math.max(...product.images.map(image => image.position)) + 1 : 0, primary };
      setStage('Subiendo imagen…');
      await uploadImageBytes(prepared.uploadUrl, file);
      const uploaded = { imageId: prepared.imageId, metadata };
      setPending(uploaded);
      await finalize(uploaded);
    } catch (problem) { setError(problem); }
    finally { setBusy(false); setStage(null); }
  }
  async function retry() {
    setBusy(true); setError(null);
    try {
      const current = await store.product(product.id);
      if (current.images.some(image => image.id === pending.imageId)) { setPending(null); reload(); }
      else {
        const metadata = { ...pending.metadata, position: current.images.length ? Math.max(...current.images.map(image => image.position)) + 1 : 0 };
        await finalize({ ...pending, metadata });
      }
    } catch (problem) { setError(problem); }
    finally { setBusy(false); setStage(null); }
  }
  async function change(image, action) {
    setBusy(true); setError(null);
    try {
      if (action === 'remove') await store.removeImage(product.id, image.id);
      else await store.primaryImage(product.id, image.id);
      reload();
    } catch (problem) { setError(problem); }
    finally { setBusy(false); }
  }
  return <section className="card"><h2>Imágenes</h2><Feedback error={error} message={stage} />
    {pending ? <div className="notice"><p>La imagen se ha subido. Falta confirmar sus metadatos.</p><button disabled={busy} onClick={retry}>Reintentar confirmación</button></div>
      : <form onSubmit={upload}>
        <label>Archivo de imagen<input ref={fileInput} type="file" accept="image/*" required disabled={busy} onChange={event => setFile(event.target.files[0] || null)} /></label>
        <label>Texto alternativo<input maxLength={255} disabled={busy} value={altText} onChange={event => setAltText(event.target.value)} /></label>
        <label className="checkbox"><input type="checkbox" disabled={busy} checked={primary} onChange={event => setPrimary(event.target.checked)} />Usar como imagen principal</label>
        <button disabled={busy}>{busy ? 'Subiendo…' : 'Subir imagen'}</button>
      </form>}
    <div className="image-list">{product.images.map(image => <article key={image.id}>
      <ProductImage product={product} image={image} /><p>{image.altText || product.name}{image.primary ? ' · Principal' : ''}</p>
      <div className="actions"><button className="secondary" disabled={busy || image.primary} onClick={() => change(image, 'primary')}>Hacer principal</button><button className="danger" disabled={busy} onClick={() => change(image, 'remove')}>Eliminar imagen</button></div>
    </article>)}</div>
    {product.images.length === 0 && <p className="muted">Todavía no hay imágenes.</p>}
  </section>;
}
