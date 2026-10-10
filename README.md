# Dummy Commerce

Proyecto de e-commerce.

## Instalación

Necesitas **Docker Desktop** en ejecución y **Node.js 22.12 o superior**.

1. Descarga o clona el repositorio y abre una terminal en la carpeta `dummy-commerce`.
2. Levanta el backend:

```sh
docker compose up -d --build --wait --wait-timeout 240
```

La primera vez tarda unos minutos. Puedes comprobar el estado con `docker compose ps`: los nueve contenedores deben aparecer como `healthy`.

3. Arranca el frontend:

```sh
cd frontend
npm install
npm run dev
```

4. Deja la terminal abierta y entra en **[http://localhost:5173](http://localhost:5173)**.

Docker prepara las bases de datos, las migraciones, Kafka, MinIO y las cuentas de administrador y cliente de prueba. No hace falta configurar nada más para probarlo.

## Probar la aplicación

### Credenciales

| Cuenta | Email | Contraseña |
| --- | --- | --- |
| Administrador | `admin@dummy-commerce.local` | `LocalAdmin123!` |
| Cliente de prueba | `clienteprueba@example.com` | `ClientePrueba!!23` |

**Ambas cuentas se crean automáticamente al arrancar con Docker Compose, también en una instalación nueva.** Inicia sesión directamente con los datos de la tabla; no necesitas registrarlas. Los reinicios no duplican las cuentas ni cambian sus contraseñas.

Si quieres probar el registro desde cero, pulsa **Crear cuenta** y utiliza otro email y contraseña. 

### Preparar un producto

Una instalación nueva empieza sin productos. Entra como administrador y:

1. En **Administración → Categorías**, crea una categoría.
2. En **Productos → Nuevo producto**, añade nombre, descripción, categoría y precio; por ejemplo, una taza de `14.90` euros.
3. Guarda el producto. En su pantalla de edición puedes subir imágenes y elegir la principal.
4. Pulsa **Gestionar stock**, introduce `10` unidades y crea el inventario.

También puedes editar productos, desactivarlos y aumentar o disminuir su stock. **Retirar definitivamente** impide volver a activar ese producto.

### Hacer una compra

1. Cierra la sesión de administrador. Puedes consultar los productos y preparar el carrito sin iniciar sesión.
2. Inicia sesión con el cliente de prueba. Si prefieres utilizar una cuenta nueva, créala desde **Crear cuenta**.
3. Añade un producto al carrito y pulsa **Confirmar compra**.
4. El pedido se actualiza automáticamente hasta quedar **Confirmado**.
5. Consulta **Mis pedidos** y **Notificaciones** para ver el resultado.

Los pagos y los emails son simulados: no se pide tarjeta ni se envían correos reales. La notificación puede tardar unos segundos.

Para comprobar la parte administrativa, copia el ID del pedido, entra como administrador y búscalo en **Administración → Pagos**. En **Inventario** verás el stock que queda.

### Otros casos para probar

- **Sin stock:** compra más unidades de las disponibles. El pedido termina cancelado.
- **Precio modificado:** prepara un carrito y cambia el precio desde otra sesión de administrador. La compra se rechaza con el precio anterior; pulsa **Actualizar carrito** para revisar el nuevo total.
- **Producto desactivado:** desactiva un producto que ya esté en un carrito. No se podrá comprar y, al actualizar el carrito, aparecerá como no disponible.

Si la app muestra un error de conexión, comprueba que Docker y la terminal del frontend siguen en ejecución.

Para detenerla, pulsa **Ctrl+C** en la terminal del frontend y ejecuta `docker compose down` desde la raíz. Los datos de PostgreSQL y MinIO se conservan; `docker compose down -v` los elimina.

## Frontend

React, Vite, JavaScript, React Router, `fetch` y CSS. Incluye la tienda pública, carrito persistente, autenticación, pedidos, notificaciones y panel de administración. El estado de los pedidos se consulta periódicamente mientras se procesa la compra.

Las URLs de las APIs se pueden configurar en `frontend/.env` tomando como referencia [frontend/.env.example](frontend/.env.example). Los valores predeterminados funcionan con Docker Compose.

## Backend

Java 21, Spring Boot 4.1.1, Maven, Spring Data JPA, Spring Security, PostgreSQL 17, Flyway, Kafka y MinIO.

El backend está dividido en seis microservicios:

| Servicio | Puerto | Responsabilidad |
| --- | --- | --- |
| Users | 8081 | Registro, login, JWT, renovación y cierre de sesión |
| Store | 8080 | Productos, categorías e imágenes |
| Orders | 8082 | Pedidos y coordinación del proceso de compra |
| Inventory | 8083 | Stock, reservas y confirmación de unidades |
| Payments | 8084 | Pagos y devoluciones simulados |
| Notifications | 8085 | Contactos de usuarios y notificaciones simuladas |

### Arquitectura y persistencia

Cada servicio organiza el código por funcionalidad y utiliza una arquitectura hexagonal: el **dominio** contiene las reglas de negocio, la **aplicación** coordina los casos de uso y los **adaptadores** conectan con HTTP, JPA, Kafka o almacenamiento. Los puertos permiten separar esas dependencias de los casos de uso.

Cada microservicio tiene su propia base de datos y usuario en PostgreSQL, alojados en una misma instancia local. Flyway gestiona las migraciones de cada módulo. Los servicios intercambian información mediante APIs y eventos, sin consultar directamente las tablas de otro servicio.

Inventory controla las reservas dentro de una transacción y bloquea las filas de stock para evitar conflictos entre compras simultáneas. Primero valida todas las líneas del pedido y después aplica la reserva completa.

### Compra distribuida: Saga

Orders coordina una Saga: una secuencia de operaciones en servicios distintos, con compensaciones cuando una parte falla.

Antes de crear el pedido, valida los productos y sus precios contra Store y obtiene el usuario del JWT. Después, la compra continúa mediante comandos y eventos de Kafka:

```text
Pedido creado
    → reserva de stock
    → pago
    → confirmación de stock
    → pedido confirmado
    → notificación
```

El pedido guarda los precios de compra para conservar su importe aunque el catálogo cambie después. Su estado refleja el avance del proceso.

Los fallos tienen una respuesta de negocio:

- Si no hay stock suficiente, el pedido se cancela.
- Si el pago falla, se libera el stock reservado antes de completar la cancelación.
- Si el pago se completa pero falla la confirmación del stock, se solicita una devolución y después se libera la reserva.
- Si la devolución falla, la compensación queda pendiente y se reintenta; el pedido no se da por cancelado antes de terminarla.

El estado de las compensaciones se persiste para poder continuar el proceso tras un reinicio.

### Kafka y fiabilidad

Los mensajes pueden entregarse más de una vez (**at-least-once**). El backend combina tres mecanismos para mantener consistencia y evitar repetir operaciones:

- **Outbox transaccional:** guarda el cambio de negocio y el mensaje pendiente en la misma transacción. Un publicador lo envía a Kafka y lo marca como publicado después de recibir confirmación. Si el envío falla, permanece pendiente.
- **Inbox e idempotencia:** cada consumer registra el identificador del mensaje junto con sus cambios. Una entrega repetida no vuelve a aplicar la operación; también se comprueban los estados de negocio.
- **Reintentos y DLT:** los errores de procesamiento tienen reintentos limitados. Los mensajes que no se pueden procesar terminan en un Dead Letter Topic para su revisión.

Inventory, Orders, Payments y Notifications permiten a ADMIN reprocesar un mensaje DLT indicando topic, partición y offset en `POST /api/admin/kafka/dlt/reprocess`. El replay se registra después del ACK de Kafka y repetir las mismas coordenadas no vuelve a publicar el mensaje.

Notifications mantiene una proyección local del email del usuario a partir de `UserRegistered`. Así puede generar los avisos de pedidos sin consultar Users en cada envío.

### Seguridad

Users emite access tokens JWT y refresh tokens. La renovación rota el refresh token, que se almacena mediante hash; el logout lo revoca.

Los servicios validan el JWT y los permisos USER/ADMIN. El catálogo es público, mientras que los pedidos y las notificaciones se consultan por propietario. Orders toma la identidad del token y rechaza productos inactivos o precios que no coincidan con Store.

Las operaciones de administración requieren ADMIN. Docker Compose crea las dos cuentas de prueba mediante configuración de arranque. Sus datos se pueden cambiar con `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` y `BOOTSTRAP_CUSTOMER_EMAIL` / `BOOTSTRAP_CUSTOMER_PASSWORD`; cada bootstrap se puede desactivar con su variable `BOOTSTRAP_ADMIN_ENABLED` o `BOOTSTRAP_CUSTOMER_ENABLED`. Fuera de Compose están desactivados por defecto. El secreto de firma se configura con `JWT_SECRET`.

### Imágenes con MinIO

Store prepara una URL firmada para subir la imagen. El navegador envía el archivo directamente a MinIO y después confirma sus metadatos en Store. Las descargas también utilizan URLs firmadas.

Los archivos se guardan en MinIO y sus referencias, posición, texto alternativo e imagen principal se guardan en PostgreSQL. El endpoint público predeterminado es `http://localhost:9000`.

### Swagger y Postman

Cada servicio ofrece `/swagger-ui/index.html`, `/v3/api-docs` y `/actuator/health` en su puerto. Por ejemplo: [Swagger de Store](http://localhost:8080/swagger-ui/index.html) y [Swagger de Users](http://localhost:8081/swagger-ui/index.html). Para las operaciones privadas, introduce un access token en **Authorize**.

En Postman, importa [la colección](postman/dummy-commerce.postman_collection.json) y [el entorno local](postman/dummy-commerce.local.postman_environment.json). La carpeta **E2E Flow** prepara un producto con imagen y stock, registra un cliente y comprueba pedido, pago y notificación. La colección también incluye autenticación, inventario y reprocesamiento DLT.

### Tests

El backend incluye tests de dominio, casos de uso, seguridad, persistencia, mensajería y compensaciones. Las pruebas de integración utilizan PostgreSQL real con Testcontainers.

Con Java 21, Maven y Docker en ejecución, desde la raíz:

```sh
mvn -f backend/users/pom.xml test
mvn -f backend/store/pom.xml test
mvn -f backend/inventory/pom.xml test
mvn -f backend/orders/pom.xml test
mvn -f backend/payments/pom.xml test
mvn -f backend/notifications/pom.xml test
```

En el frontend, desde `frontend`: `npm run build`, `npm run lint` y `npm test`.

## Estructura

```text
backend/                  Los seis microservicios
frontend/                 Aplicación React
infrastructure/postgres/  Inicialización de bases y permisos
postman/                  Colección y entorno de pruebas
docker-compose.yml        Servicios e infraestructura local
```
