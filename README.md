# Dummy Commerce

Backend de e-commerce distribuido en seis microservicios. Incluye autenticación, catálogo con imágenes, inventario, pedidos, pagos simulados y notificaciones simuladas. Los pedidos coordinan una Saga con Kafka, incluidas las compensaciones de stock y refund.

## Stack y arquitectura

Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 17, Flyway, Spring Data JPA, Spring Security, JWT, Kafka KRaft, MinIO, springdoc/OpenAPI, Testcontainers y Docker Compose.

Cada microservicio organiza el código por funcionalidad y separa dominio, aplicación y adaptadores con una arquitectura hexagonal pragmática. PostgreSQL aloja una base y un usuario propios para cada servicio; las migraciones pertenecen a su módulo.

Orders valida productos activos y precios contra Store antes de aceptar el checkout. Después coordina la reserva de stock, el pago y la confirmación mediante Kafka. Un fallo de pago libera stock; un fallo de confirmación después del cobro requiere refund antes de completar la cancelación. Notifications mantiene una proyección local del email del usuario a partir de `UserRegistered`.

Los cambios de negocio y Outbox se guardan en la misma transacción. Cada consumer registra Inbox junto con sus cambios y nuevos mensajes. La entrega es **at-least-once**, con procesamiento idempotente, tres reintentos por defecto y DLT. La retención predeterminada es de 30 días para Outbox publicado y 90 días para Inbox; los mensajes pendientes no se eliminan.

## Arranque local

Requiere Docker con Compose v2. Para ejecutar los tests desde el host también hacen falta Java 21 y Maven. Conviene asignar al menos 8 GB de memoria a Docker para levantar todos los servicios juntos.

Desde la raíz:

```sh
docker compose build
docker compose up -d --wait --wait-timeout 240
docker compose ps
```

El arranque inicial crea automáticamente las seis bases y sus permisos, aplica Flyway, configura los topics y crea el bucket de imágenes. No requiere SQL manual ni crear el bucket desde la consola.

| Servicio | Puerto local | Responsabilidad |
| --- | --- | --- |
| Store | 8080 | Productos, categorías e imágenes |
| Users | 8081 | Registro, login, JWT, refresh y logout |
| Orders | 8082 | Pedidos y Saga |
| Inventory | 8083 | Stock y reservas |
| Payments | 8084 | Pagos y refunds simulados |
| Notifications | 8085 | Proyección de usuarios y envío simulado |
| PostgreSQL | 5432 | Bases de los servicios |
| Kafka | 9092 | Broker accesible desde el host |
| MinIO | 9000 / 9001 | API S3 / consola |

`docker compose down` conserva los datos. `docker compose down -v` elimina los volúmenes de este entorno local y permite repetir una instalación limpia. Los nombres predeterminados son `dummy-commerce_backend_postgres_data` y `dummy-commerce_backend_minio_data`.

## Configuración y autenticación

Compose incluye credenciales de desarrollo. Las variables pueden definirse en el entorno o en un `.env` local.

| Variable | Uso / valor local predeterminado |
| --- | --- |
| `JWT_SECRET` | Secreto HMAC compartido, Base64 de al menos 32 bytes; Compose incluye uno de desarrollo |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://localhost:4200` |
| `BOOTSTRAP_ADMIN_ENABLED` | `true` en Compose; desactivado por defecto fuera de Compose |
| `BOOTSTRAP_ADMIN_EMAIL` | `admin@dummy-commerce.local` |
| `BOOTSTRAP_ADMIN_PASSWORD` | `LocalAdmin123!` |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `minioadmin` / `minioadmin123` |
| `MINIO_PUBLIC_ENDPOINT` | `http://localhost:9000`, accesible desde el cliente |
| `MINIO_REGION` | `us-east-1` |
| `PAYMENT_SIMULATOR_MODE` / `NOTIFICATION_SIMULATOR_MODE` | `success` |
| `POSTGRES_VOLUME_NAME` / `MINIO_VOLUME_NAME` | Nombres alternativos para los volúmenes locales |

Store usa `http://minio:9000` dentro de Docker y firma las URLs de subida y descarga con el endpoint público. La imagen MinIO está fijada por versión y digest, usando la distribución de [Coollabs](https://github.com/coollabsio/minio).

El ADMIN inicial se crea mediante configuración y puede iniciar sesión en `POST /api/auth/login` de Users. El bootstrap es idempotente y rechaza reutilizar una cuenta USER como ADMIN. Los usuarios normales se registran mediante `POST /api/auth/register`; las peticiones autenticadas usan `Authorization: Bearer <accessToken>`.

El catálogo es público. Cada USER consulta sus propios pedidos y notificaciones mediante `/api/orders/me` y `/api/notifications/me`. La administración, las operaciones manuales de Saga y el reprocesamiento DLT requieren ADMIN. Orders obtiene el propietario del JWT y devuelve 409 si el precio enviado no coincide con el catálogo o el producto está inactivo.

## Swagger y Postman

Cada microservicio expone `/v3/api-docs`, `/swagger-ui/index.html` y `/actuator/health` en su puerto. Swagger permite introducir un Bearer token desde **Authorize**.

Importar estos dos archivos en Postman y seleccionar el environment local:

- `postman/dummy-commerce.postman_collection.json`
- `postman/dummy-commerce.local.postman_environment.json`

La colección cubre Auth, Store, Inventory, Orders, Payments, Notifications, health, OpenAPI y DLT. Guarda tokens e IDs durante la ejecución. Ejecutar la carpeta **E2E Flow** con Collection Runner para crear un producto con imagen y stock, registrar un USER y esperar el pedido confirmado, el pago completado y la notificación enviada.

Los consumers de Inventory, Orders, Payments y Notifications ofrecen `POST /api/admin/kafka/dlt/reprocess`, con `{ "topic": "<topic-original>.DLT", "partition": 0, "offset": 0 }`. La intervención es explícita y limitada a sus topics. Conserva key y payload y registra el replay después del ACK de Kafka. Repetir las mismas coordenadas no vuelve a publicar; el registro original permanece en Kafka hasta su retención.

## Tests

Con Docker en ejecución:

```sh
mvn -f backend/users/pom.xml test
mvn -f backend/store/pom.xml test
mvn -f backend/inventory/pom.xml test
mvn -f backend/orders/pom.xml test
mvn -f backend/payments/pom.xml test
mvn -f backend/notifications/pom.xml test
```

Las suites incluyen tests de dominio, aplicación, seguridad y persistencia con PostgreSQL real en Testcontainers, además de almacenamiento MinIO, mensajería, idempotencia y compensaciones.

## Frontend local

Con el backend levantado, ejecutar en otra terminal (Node.js 20.19+ o 22.12+):

```sh
cd frontend
npm install
npm run dev
```

Abrir `http://localhost:5173`. React, Vite, JavaScript y CSS sencillo ofrecen catálogo, carrito, compra con seguimiento de la Saga, pedidos y notificaciones. ADMIN dispone de productos, categorías, imágenes MinIO, stock y consulta de pagos. Las URLs de los seis servicios se configuran con las variables `VITE_*_API_URL` de `frontend/.env.example`; los defaults corresponden a Compose. Para verificaciones: `npm run build`, `npm run lint` y `npm test`.

## Estructura

```text
backend/                  users, store, inventory, orders, payments, notifications
frontend/                 React, Vite, JavaScript y CSS
infrastructure/postgres/  Inicialización de bases y permisos
postman/                  Colección y environment local
docker-compose.yml        Entorno completo del backend
```
