import { useState } from 'react';
import { imageUrl } from '../api/index.js';

export default function ProductImage({ product, image, className = '' }) {
  const selected = image || product?.images?.find(item => item.primary) || product?.images?.[0];
  const [failedUrl, setFailedUrl] = useState(null);
  const url = imageUrl(selected);
  return <div className={`product-image ${className}`}>
    {url && failedUrl !== url
      ? <img src={url} alt={selected.altText || product?.name || 'Imagen del producto'} onError={() => setFailedUrl(url)} />
      : <span>{url ? 'Imagen no disponible' : 'Sin imagen'}</span>}
  </div>;
}
