import { session } from '../auth/session.js';
import { createApiClient } from './client.js';

const env = import.meta.env || {};
export const urls = {
  users: env.VITE_USERS_API_URL || 'http://localhost:8081',
  store: env.VITE_STORE_API_URL || 'http://localhost:8080',
  inventory: env.VITE_INVENTORY_API_URL || 'http://localhost:8083',
  orders: env.VITE_ORDERS_API_URL || 'http://localhost:8082',
  payments: env.VITE_PAYMENTS_API_URL || 'http://localhost:8084',
  notifications: env.VITE_NOTIFICATIONS_API_URL || 'http://localhost:8085',
};
Object.keys(urls).forEach(key => { urls[key] = urls[key].replace(/\/$/, ''); });
export const api = createApiClient({ urls, session });
const get = (service, path, signal, auth = true) => api.request(service, path, { signal, auth });
const write = (service, path, method, body, auth = true) => api.request(service, path, { method, body, auth });

export const users = {
  login: body => write('users', '/api/auth/login', 'POST', body, false),
  register: body => write('users', '/api/auth/register', 'POST', body, false),
  me: signal => get('users', '/api/users/me', signal),
  logout: refreshToken => write('users', '/api/auth/logout', 'POST', { refreshToken }, false),
};
export const store = {
  products: signal => get('store', '/api/products', signal, false),
  categories: signal => get('store', '/api/categories', signal, false),
  product: (id, signal) => get('store', `/api/products/${id}`, signal, false),
  createCategory: body => write('store', '/api/categories', 'POST', body),
  createProduct: body => write('store', '/api/products', 'POST', body),
  updateProduct: (id, body) => write('store', `/api/products/${id}`, 'PUT', body),
  changeStatus: (id, action) => write('store', `/api/products/${id}/${action}`, 'PATCH'),
  prepareImage: id => write('store', `/api/products/${id}/images/upload-url`, 'POST'),
  confirmImage: (id, imageId, body) => write('store', `/api/products/${id}/images/${imageId}/confirm`, 'POST', body),
  primaryImage: (id, imageId) => write('store', `/api/products/${id}/images/${imageId}/primary`, 'PATCH'),
  removeImage: (id, imageId) => write('store', `/api/products/${id}/images/${imageId}`, 'DELETE'),
};
export const orders = {
  mine: signal => get('orders', '/api/orders/me', signal),
  order: (id, signal) => get('orders', `/api/orders/${id}`, signal),
  create: items => write('orders', '/api/orders', 'POST', { items }),
};
export const notifications = { mine: signal => get('notifications', '/api/notifications/me', signal) };
export const inventory = {
  item: (id, signal) => get('inventory', `/api/inventory/${id}`, signal),
  create: body => write('inventory', '/api/inventory', 'POST', body),
  change: (id, action, quantity) => write('inventory', `/api/inventory/${id}/${action}`, 'PATCH', { quantity }),
};
export const payments = { forOrder: (id, signal) => get('payments', `/api/payments/order/${id}`, signal) };

export async function loadCatalog(signal) {
  const [products, categories] = await Promise.all([store.products(signal), store.categories(signal)]);
  return { products, categories };
}
export function imageUrl(image) {
  return image ? new URL(image.contentUrl, `${urls.store}/`).href : null;
}

export async function uploadImageBytes(uploadUrl, file) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 30000);
  try {
    const response = await fetch(uploadUrl, {
      method: 'PUT', body: file, signal: controller.signal,
      headers: { 'Content-Type': file.type || 'application/octet-stream' },
    });
    if (!response.ok) throw new Error('MinIO no ha podido guardar la imagen. Puedes volver a intentar la subida.');
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('La subida ha superado el tiempo de espera. Inténtalo de nuevo.', { cause: error });
    if (error instanceof TypeError) throw new Error('No se puede conectar con MinIO para subir la imagen.', { cause: error });
    throw error;
  } finally { clearTimeout(timer); }
}
