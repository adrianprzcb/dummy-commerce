import { createContext, useContext, useEffect, useState } from 'react';
import { validQuantity } from '../format.js';

const CartContext = createContext(null);
function readCart() {
  try {
    const data = JSON.parse(localStorage.getItem('dummy-commerce-cart'));
    return Array.isArray(data) ? data.filter(item => typeof item.productId === 'string' && validQuantity(item.quantity)
      && Number.isFinite(item.unitPrice) && item.unitPrice >= 0) : [];
  } catch { return []; }
}
export function CartProvider({ children }) {
  const [items, setItems] = useState(readCart);
  useEffect(() => {
    try { localStorage.setItem('dummy-commerce-cart', JSON.stringify(items)); } catch { /* El carrito también funciona en memoria. */ }
  }, [items]);
  function add(product, quantity) {
    if (!validQuantity(quantity)) throw new Error('La cantidad debe estar entre 1 y 999.');
    const existing = items.find(item => item.productId === product.id);
    if (existing && !validQuantity(existing.quantity + quantity)) throw new Error('Máximo 999 unidades por producto.');
    setItems(previous => {
      const found = previous.find(item => item.productId === product.id);
      return found ? previous.map(item => item.productId === product.id
        ? { ...item, quantity: item.quantity + quantity, unitPrice: product.price, status: product.status } : item)
        : [...previous, { productId: product.id, name: product.name, unitPrice: product.price, quantity, status: product.status }];
    });
  }
  function quantity(productId, value) {
    if (validQuantity(value)) setItems(previous => previous.map(item => item.productId === productId ? { ...item, quantity: value } : item));
  }
  return <CartContext.Provider value={{ items, add, quantity, replace: setItems,
    remove: id => setItems(previous => previous.filter(item => item.productId !== id)), clear: () => setItems([]) }}>{children}</CartContext.Provider>;
}
export const useCart = () => useContext(CartContext);
