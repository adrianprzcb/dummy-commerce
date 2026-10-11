export function productAvailability(product) {
  if (product.status !== 'ACTIVE') return product.status;
  if (!Number.isInteger(product.availableQuantity) || product.availableQuantity < 0) return 'STOCK_UNKNOWN';
  return product.availableQuantity === 0 ? 'OUT_OF_STOCK' : 'IN_STOCK';
}
