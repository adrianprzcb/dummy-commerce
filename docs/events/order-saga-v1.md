# Order Saga v1

Orders orquesta la Saga. Los contratos JSON se representan localmente en cada
microservicio; no existe un JAR de eventos compartido.

## Flujo

`CREATED → ReserveStock → StockReserved → STOCK_RESERVED → ProcessPayment
→ PaymentCompleted → PAYMENT_COMPLETED → ConfirmStock → StockConfirmed
→ CONFIRMED → OrderConfirmed`.

- Reserva rechazada: `StockReservationFailed → CANCELLED → OrderCancelled`,
  con el motivo de la reserva.
- Pago rechazado: `PaymentFailed → ReleaseStock → StockReleased → CANCELLED
  → OrderCancelled`, con motivo `PAYMENT_FAILED`. El pedido sigue
  `STOCK_RESERVED` mientras espera la liberación. `order_compensations` conserva
  esa espera incluso tras reiniciar Orders y evita cancelar por una liberación
  ajena a la compensación.
- Inventory reserva todos los productos o ninguno: bloquea sus filas en orden
  determinista, comprueba el lote completo y después guarda todas las reservas.
  Confirmación y liberación mantienen el orden de bloqueo de las operaciones
  REST existentes: reservas por producto, después inventario por producto.

## Topics y contratos

Todos los contratos incluyen `messageId: UUID`, `orderId: UUID` y
`occurredAt: Instant` (ISO 8601). Se envían como JSON String; la Kafka key es
siempre `orderId.toString()`. Los listeners comprueban que key y payload coincidan.

| Topic exacto | Contrato | Campos adicionales |
| --- | --- | --- |
| `dummy-commerce.inventory.reserve-stock.v1` | ReserveStockCommandV1 | items: [{productId: UUID, quantity: int positivo}] |
| `dummy-commerce.inventory.stock-reserved.v1` | StockReservedEventV1 | — |
| `dummy-commerce.inventory.stock-reservation-failed.v1` | StockReservationFailedEventV1 | reason: String |
| `dummy-commerce.payments.process-payment.v1` | ProcessPaymentCommandV1 | amount: decimal, currency: String |
| `dummy-commerce.payments.payment-completed.v1` | PaymentCompletedEventV1 | paymentId: UUID, amount: decimal, currency: String |
| `dummy-commerce.payments.payment-failed.v1` | PaymentFailedEventV1 | paymentId: UUID, amount: decimal, currency: String, reason: String |
| `dummy-commerce.inventory.confirm-stock.v1` | ConfirmStockCommandV1 | — |
| `dummy-commerce.inventory.stock-confirmed.v1` | StockConfirmedEventV1 | — |
| `dummy-commerce.inventory.release-stock.v1` | ReleaseStockCommandV1 | — |
| `dummy-commerce.inventory.stock-released.v1` | StockReleasedEventV1 | — |
| `dummy-commerce.orders.order-confirmed.v1` | OrderConfirmedEventV1 | userId: UUID |
| `dummy-commerce.orders.order-cancelled.v1` | OrderCancelledEventV1 | userId: UUID, reason: String |

Orders usa el total persistido del pedido y `PlatformCurrency.EUR`. Payments
conserva el procesador simulado y su idempotencia de negocio por `orderId`.

## Entrega y transacciones

- Orders, Inventory y Payments guardan negocio + Outbox en la misma transacción
  PostgreSQL. Solo `OutboxPublisher` utiliza `KafkaTemplate`.
- El publicador lee hasta 100 mensajes PENDING ordenados por `createdAt` e ID,
  con bloqueo pesimista. Espera el ACK antes de marcar PUBLISHED y guardar
  `publishedAt`. Ante fallo, timeout o interrupción conserva PENDING y detiene
  el lote; el siguiente ciclo vuelve a intentar. Mantiene los bloqueos hasta
  el commit para serializar publicadores concurrentes.
- Inbox reclama `messageId` mediante `INSERT ... ON CONFLICT DO NOTHING`.
  La reclamación, negocio y posibles mensajes Outbox comparten la transacción.
  La fila Inbox solo es visible si todo hace commit. Una entrega concurrente
  espera la resolución de la primera transacción; un rollback permite reintento.
- El listener delega a un bean transaccional y usa ACK por registro. Los errores
  técnicos llegan al contenedor; el manejador básico reintenta sin descartar
  mensajes. Un mensaje inválido bloqueará su partición hasta corregir la causa;
  la política de DLQ queda pendiente.
- La entrega es **at-least-once**. Un ACK Kafka seguido de un fallo de commit
  puede repetir el envío, manteniendo el mismo messageId. Inbox evita repetir
  el efecto. No se garantiza exactly-once entre servicios.

Configuración local: `KAFKA_BOOTSTRAP_SERVERS` (localhost:9092 por defecto),
`OUTBOX_PUBLISHER_ENABLED` (true), `OUTBOX_PUBLISHER_FIXED_DELAY_MS` (1000),
`OUTBOX_PUBLISHER_SEND_TIMEOUT_MS` (10000). Cada micro tiene su grupo
`dummy-commerce.<micro>.saga.v1`. Los tests de PostgreSQL desactivan los listeners
y el publicador programado, y verifican los envíos con mocks.

## Pendiente fuera de esta fase

Notifications Kafka consumer pendiente de decidir cómo obtener el recipient fiable desde Users.
Los eventos de Orders no incluyen email; no se inventa un recipient ni se añade
una llamada REST a Users. También quedan pendientes el broker en Compose, el E2E
con Kafka real, DLQ, limpieza de Inbox/Outbox y compensaciones tras un pago completado.
