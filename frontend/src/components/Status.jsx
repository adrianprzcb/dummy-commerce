const labels = {
  CREATED: 'Procesando', STOCK_RESERVED: 'Stock reservado', PAYMENT_COMPLETED: 'Pago completado',
  CONFIRMED: 'Confirmado', CANCELLED: 'Cancelado', ACTIVE: 'Activo',
  INACTIVE: 'Inactivo', DISCONTINUED: 'Retirado', PENDING: 'Pendiente', SENT: 'Enviada',
  FAILED: 'Fallido', COMPLETED: 'Completado', REFUNDED: 'Reembolsado',
};
export default function Status({ value }) {
  return <span className={`status status-${value?.toLowerCase()}`}>{labels[value] || value}</span>;
}
