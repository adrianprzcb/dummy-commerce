const labels = {
  CREATED: 'Procesando', STOCK_RESERVED: 'Stock reservado', PAYMENT_COMPLETED: 'Pago completado',
  CONFIRMED: 'Confirmado', CANCELLED: 'Cancelado', ACTIVE: 'Activo',
  INACTIVE: 'Inactivo', DISCONTINUED: 'Retirado', PENDING: 'Pendiente', SENT: 'Enviado',
  FAILED: 'Fallido', COMPLETED: 'Completado', REFUNDED: 'Reembolsado',
  IN_STOCK: 'Disponible', OUT_OF_STOCK: 'Fuera de stock', STOCK_UNKNOWN: 'Stock sin confirmar',
};
export default function Status({ value }) {
  return <span className={`status status-${value?.toLowerCase()}`}>{labels[value] || value}</span>;
}
