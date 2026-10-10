export const money = value => new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
export const dateTime = value => new Date(value).toLocaleString('es-ES');
export const shortId = value => value?.slice(0, 8);

export function validQuantity(value) {
  return Number.isInteger(value) && value >= 1 && value <= 999;
}
export function cartTotal(items) {
  return items.reduce((sum, item) => sum + Math.round(item.unitPrice * 100) * item.quantity, 0) / 100;
}
