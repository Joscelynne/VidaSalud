# Guía de Integración RabbitMQ — VidaSalud

Documentación técnica integral sobre la arquitectura de mensajería asíncrona, topología de colas, Dead Letter Queues (DLQ), confirmación manual ACK/NACK, API administrativa REST, seguridad con Microsoft Entra ID y preparación para cluster de alta disponibilidad (EP4).

---

## 1. Arquitectura de Mensajería Asíncrona

```
[ BFF Controller / Productores ]
            │
            ├─────────────────────────────────────────┐
            │ (citas.crear / citas.confirmar)         │ (vidasalud.notificacion.*)
            ▼                                         ▼
[ vidasalud.direct.exchange ]             [ vidasalud.topic.exchange ]
      │                   │                           │
      │ citas.crear       │ citas.confirmar           │ vidasalud.notificacion.*
      ▼                   ▼                           ▼
[ citas.agendamiento.queue ] [ citas.confirmacion.queue ] [ notificaciones.auditoria.queue ]
      │                              │                             │
      │ (Fallo / Error NACK)         │ (Fallo / Error NACK)        │ (Fallo / Error NACK)
      └──────────────────────────────┼─────────────────────────────┘
                                     ▼
                        [ vidasalud.dlx.exchange ]
                                     │
           ┌─────────────────────────┼─────────────────────────┐
           │ citas.agendamiento.dlq.rk│ citas.confirmacion.dlq.rk│ notificaciones.auditoria.dlq.rk
           ▼                         ▼                         ▼
[ citas.agendamiento.dlq ] [ citas.confirmacion.dlq ] [ notificaciones.auditoria.dlq ]
```

---

## 2. Topología RabbitMQ

### 2.1 Exchanges
| Nombre Exchange | Tipo | Propósito | Durabilidad |
|---|---|---|---|
| `vidasalud.direct.exchange` | `direct` | Enrutamiento punto a punto para creación y confirmación de citas | Durable |
| `vidasalud.topic.exchange` | `topic` | Enrutamiento jerárquico para notificaciones y auditoría | Durable |
| `vidasalud.dlx.exchange` | `direct` | Dead Letter Exchange para canalizar mensajes fallidos hacia las DLQ | Durable |

### 2.2 Colas Principales (Durables)
Todas las colas principales están configuradas con `x-dead-letter-exchange: vidasalud.dlx.exchange` y su respectiva routing key DLQ:

| Cola Principal | Exchange Origen | Routing Key Binding | x-dead-letter-routing-key |
|---|---|---|---|
| `citas.agendamiento.queue` | `vidasalud.direct.exchange` | `citas.crear` | `citas.agendamiento.dlq.rk` |
| `citas.confirmacion.queue` | `vidasalud.direct.exchange` | `citas.confirmar` | `citas.confirmacion.dlq.rk` |
| `notificaciones.auditoria.queue` | `vidasalud.topic.exchange` | `vidasalud.notificacion.*` | `notificaciones.auditoria.dlq.rk` |

### 2.3 Dead Letter Queues (DLQ - Durables)
Las DLQs almacenan los mensajes no procesables para auditoría, inspección y reintento manual sin bloquear los consumidores principales:

| Dead Letter Queue (DLQ) | Exchange DLX | Routing Key Binding |
|---|---|---|
| `citas.agendamiento.dlq` | `vidasalud.dlx.exchange` | `citas.agendamiento.dlq.rk` |
| `citas.confirmacion.dlq` | `vidasalud.dlx.exchange` | `citas.confirmacion.dlq.rk` |
| `notificaciones.auditoria.dlq` | `vidasalud.dlx.exchange` | `notificaciones.auditoria.dlq.rk` |

---

## 3. Variables de Entorno

| Variable | Descripción | Valor Predeterminado (Local) |
|---|---|---|
| `SPRING_RABBITMQ_HOST` | Host del broker RabbitMQ | `localhost` (en Docker: `rabbitmq`) |
| `SPRING_RABBITMQ_PORT` | Puerto de conexión AMQP | `5672` |
| `SPRING_RABBITMQ_USERNAME` | Usuario del broker | `vidasalud` |
| `SPRING_RABBITMQ_PASSWORD` | Contraseña de desarrollo local | `vidasalud_local_2026` |
| `SPRING_RABBITMQ_VIRTUAL_HOST` | Virtual host en RabbitMQ | `/` |
| `SPRING_RABBITMQ_LISTENER_PREFETCH` | Prefetch count de consumidores | `10` |
| `SPRING_RABBITMQ_RETRY_MAX_ATTEMPTS` | Intentos máximos de retry en listener | `3` |

> [!CAUTION]
> La contraseña `vidasalud_local_2026` es exclusiva para entornos de desarrollo local en contenedores aislados. Nunca debe utilizarse en entornos de producción o staging.

---

## 4. Guía de Ejecución Rápida en macOS

### 4.1 Iniciar el Contenedor RabbitMQ
```bash
docker compose up -d rabbitmq
```

Verificar que el contenedor esté saludable (`healthy`):
```bash
docker ps --filter "name=vidasalud-rabbitmq"
```

Acceder al panel web RabbitMQ Management (solo disponible en local):
- URL: `http://localhost:15672`
- Usuario: `vidasalud`
- Contraseña: `vidasalud_local_2026`

### 4.2 Iniciar el Backend BFF
```bash
cd ms-vidasalud-bff
mvn clean spring-boot:run
```

Al iniciar el BFF, la clase `RabbitMQConfig` y `RabbitAdmin` declararán de forma automática los 3 exchanges, las 6 colas durables y los 6 bindings en el broker.

### 4.3 Comprobar las Colas desde la Terminal
```bash
docker exec vidasalud-rabbitmq rabbitmqctl list_queues name messages consumers
```

---

## 5. Funcionamiento de ACK, NACK, Reintentos y DLQ

### 5.1 Flujo de Procesamiento Exitoso (ACK)
1. El consumidor (`@RabbitListener`) recibe el mensaje y su `deliveryTag` asociado.
2. Se ejecuta la validación y la lógica de negocio.
3. Se invoca `channel.basicAck(deliveryTag, false)`.
4. El mensaje es retirado de la cola de forma definitiva.

### 5.2 Flujo de Fallo Definitivo y Redirección a DLQ (NACK)
1. Si el mensaje es nulo, inválido o falla por reglas de negocio definitivas, se captura la excepción.
2. El consumidor ejecuta:
   ```java
   channel.basicNack(deliveryTag, false, false);
   ```
   - Primer argumento: `deliveryTag` (identificador único del mensaje en el canal).
   - Segundo argumento: `multiple = false` (solo aplica al mensaje actual).
   - Tercer argumento: `requeue = false` (evita reenviar a la misma cola, impidiendo bucles infinitos).
3. RabbitMQ detecta el rechazo con `requeue=false` e inspecciona los argumentos de la cola (`x-dead-letter-exchange` y `x-dead-letter-routing-key`).
4. El broker enruta automáticamente el mensaje fallido hacia `vidasalud.dlx.exchange`, el cual lo deposita en la DLQ correspondiente (`citas.agendamiento.dlq`).

### 5.3 Simulación Controlada de Fallos para Pruebas
Para probar que un mensaje viaja a la DLQ de forma controlada en desarrollo o pruebas:
- Incluir en `notes` el texto `"SIMULATE_FAIL"`.
- O prefijar el `appointmentId` con `"FAIL-"` (ej. `"FAIL-APT-999"`).

El consumidor detecta este patrón, lanza una excepción de validación y ejecuta `basicNack(deliveryTag, false, false)`, enviándolo a la DLQ.

---

## 6. API REST Administrativa (`/api/admin/amqp/**`)

Toda la API administrativa está protegida y exige exclusivamente el rol **Admin** autenticado con Microsoft Entra ID.

### 6.1 Endpoints Disponibles
| Método | Ruta | Descripción | Código Éxito |
|---|---|---|---|
| `POST` | `/api/admin/amqp/queues` | Declarar una nueva cola (soporta DLQ, TTL, Max Length) | `201 Created` |
| `GET` | `/api/admin/amqp/queues/{name}` | Consultar estadísticas de una cola (mensajes y consumidores) | `200 OK` |
| `DELETE` | `/api/admin/amqp/queues/{name}` | Eliminar una cola existente | `200 OK` |
| `POST` | `/api/admin/amqp/queues/{name}/purge` | Purgar mensajes de una cola | `200 OK` |
| `POST` | `/api/admin/amqp/exchanges` | Declarar un exchange (direct, topic o fanout) | `201 Created` |
| `POST` | `/api/admin/amqp/bindings` | Declarar un binding entre exchange y cola | `201 Created` |
| `POST` | `/api/admin/amqp/test-publish` | Publicar un mensaje de prueba en exchange y routing key | `200 OK` |

### 6.2 Ejemplos cURL

#### Consultar Estadísticas de Cola:
```bash
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>"
```

#### Declarar una Nueva Cola con DLQ:
```bash
curl -i -X POST http://localhost:8080/api/admin/amqp/queues \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "citas.urgencia.queue",
    "durable": true,
    "deadLetterExchange": "vidasalud.dlx.exchange",
    "deadLetterRoutingKey": "citas.urgencia.dlq.rk",
    "messageTtl": 86400000
  }'
```

#### Publicar Mensaje de Prueba:
```bash
curl -i -X POST http://localhost:8080/api/admin/amqp/test-publish \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "exchange": "vidasalud.direct.exchange",
    "routingKey": "citas.crear",
    "payload": {
      "appointmentId": "APT-TEST-001",
      "patientName": "Juan Pérez",
      "doctorName": "Dra. Paz",
      "specialty": "Medicina General",
      "status": "PENDING"
    }
  }'
```

#### Probar Enrutamiento a DLQ mediante Publicación de Fallo Controlado:
```bash
curl -i -X POST http://localhost:8080/api/admin/amqp/test-publish \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "exchange": "vidasalud.direct.exchange",
    "routingKey": "citas.crear",
    "payload": {
      "appointmentId": "FAIL-PRUEBA-001",
      "notes": "SIMULATE_FAIL"
    }
  }'
```
Tras ejecutar este comando, consultar la DLQ:
```bash
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>"
```
El campo `messageCount` aumentará a 1.

#### Purgar una Cola:
```bash
curl -i -X POST http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq/purge \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>"
```

---

## 7. Ejecución de Pruebas Automatizadas

Para compilar y ejecutar la suite completa de pruebas unitarias, de seguridad y de integración:

```bash
cd ms-vidasalud-bff
mvn clean test
```

### Cobertura de la Suite de Pruebas:
1. `RabbitMQConfigUnitTest`: Valida los 3 exchanges, las 6 colas durables, los argumentos DLX y los 6 bindings.
2. `EventAndProducerUnitTest`: Valida publicación tipada, routing keys, y serialización Jackson JSON con soporte JSR-310 (Instant).
3. `ConsumerAckNackUnitTest`: Valida confirmación manual ACK ante eventos válidos y NACK sin requeue ante eventos inválidos o simulaciones de fallo.
4. `RabbitAdminServiceUnitTest`: Valida operaciones administrativas de colas, exchanges, bindings, purga, eliminación y excepciones `QueueNotFoundException`.
5. `RabbitAdminControllerSecurityTest`: Valida rechazo 401 sin token, rechazo 403 para roles sin privilegios (`Paciente`, `Recepcionista`, `Auditor`), autorización 200/201 para `Admin`, validaciones 400 y errores 404.
6. `RabbitMQDeadLetterIntegrationTest`: Prueba de integración real que publica un evento fallido (`FAIL-`), verifica el NACK del consumidor y comprueba su llegada efectiva a la DLQ `citas.agendamiento.dlq`.

---

## 8. Limitaciones Conocidas

1. **Estadísticas AMQP vs. RabbitMQ Management API**:
   - `RabbitAdmin` utiliza el comando nativo `queue.declare(passive=true)` del protocolo AMQP 0-9-1. Este comando expone únicamente `messageCount` y `consumerCount`.
   - Métricas extendidas como consumo de memoria RAM por cola, I/O en disco, tasas de entrada/salida (msg/sec) y estados de sincronización de quorum requieren consultar la API HTTP del plugin de Management (`http://localhost:15672/api/queues`) o el exportador de Prometheus en el puerto `15692`.
2. **Disponibilidad del Broker y Fallback**:
   - Si RabbitMQ no está disponible, las peticiones a la API administrativa retornan `503 Service Unavailable` estandarizado mediante `GlobalExceptionHandler`.
   - La autenticación de usuarios mediante Microsoft Entra ID y los endpoints de consulta habituales continúan funcionando con normalidad.

---

## 9. Preparación para EP4: Cluster de Alta Disponibilidad (2 Nodos)

El archivo [`docker-compose.cluster.yml`](file:///Users/joscelynne/Documents/github/Cloud%20Native%20I/docker-compose.cluster.yml) contiene la arquitectura base para desplegar un cluster RabbitMQ de dos nodos en un entorno de pruebas aislado.

### 9.1 Conceptos Fundamentales
- **Cluster de Nodos**: Conecta múltiples instancias de RabbitMQ que comparten esquema, usuarios, exchanges y metadatos mediante una cookie de Erlang compartida (`RABBITMQ_ERLANG_COOKIE`).
- **Limitación de 2 Nodos**: Un cluster de dos nodos **no** garantiza tolerancia total a fallos en caso de partición de red (Split-Brain), ya que no existe quórum de mayoría (2/2 = 100%). Para producción se recomienda un mínimo de 3 nodos.
- **Quorum Queues vs. Colas Clásicas**:
  - Las colas clásicas residen en un solo nodo (el nodo donde se declararon). Si ese nodo cae, la cola queda inaccesible hasta que se recupere.
  - Las **Quorum Queues** utilizan el algoritmo de consenso Raft para replicar mensajes a través de los nodos del cluster, tolerando la pérdida de nodos sin pérdida de datos.
- **Conectividad del BFF**:
  - Para conectar el BFF a un cluster con múltiples nodos, se configuran las direcciones agrupadas:
    ```yaml
    spring:
      rabbitmq:
        addresses: rabbitmq1:5672,rabbitmq2:5672
    ```
  - El driver AMQP de Spring Boot alternará automáticamente y se reconectará ante la caída de cualquiera de los nodos.

### 9.2 Comandos de Verificación del Cluster
Para iniciar el cluster preparado para EP4:
```bash
docker compose -f docker-compose.cluster.yml up -d
```

Verificar el estado del cluster:
```bash
docker exec vidasalud-rabbitmq-node1 rabbitmqctl cluster_status
```


---

## 10. Guía de Demo para EP4 (5-10 minutos)

Esta sección proporciona un guión estructurado para demostrar la integración de RabbitMQ en la evaluación EP4.

### 10.1 Pre-requisitos de la Demo

```bash
# 1. Verificar que RabbitMQ esté corriendo
docker ps --filter "name=rabbitmq"

# 2. Verificar que el BFF esté corriendo y conectado
curl -s http://localhost:8080/actuator/health | jq .

# 3. Obtener un token JWT de Admin desde Azure (o usar token de prueba)
export ADMIN_TOKEN="<tu-token-jwt-admin>"
```

### 10.2 Secuencia de Demo (Paso a Paso)

#### Paso 1: Mostrar Topología (1 min)
```bash
# Listar exchanges declarados
docker exec vidasalud-rabbitmq rabbitmqctl list_exchanges name type | grep vidasalud

# Listar colas declaradas (6 colas: 3 principales + 3 DLQ)
docker exec vidasalud-rabbitmq rabbitmqctl list_queues name messages consumers
```

**Explicar**: "Tenemos 3 exchanges (direct, topic, dlx) y 6 colas durables. Las colas principales tienen configurado x-dead-letter-exchange para redirección automática de mensajes fallidos."

#### Paso 2: Demostrar Flujo Exitoso con ACK (2 min)
```bash
# Publicar mensaje válido de creación de cita
curl -X POST http://localhost:8080/api/admin/amqp/test-publish \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "exchange": "vidasalud.direct.exchange",
    "routingKey": "citas.crear",
    "payload": {
      "eventId": "evt-demo-001",
      "appointmentId": "APT-DEMO-001",
      "patientName": "María González",
      "doctorName": "Dr. Fernández",
      "specialty": "Cardiología",
      "dateTime": "2026-10-20T09:00:00",
      "status": "PENDING",
      "notes": "Cita de control"
    }
  }'
```

**Verificar en logs del BFF**:
```bash
docker logs vidasalud-bff --tail 20 | grep -E "ACK|APT-DEMO"
```

**Explicar**: "El consumidor recibió el mensaje, lo validó, procesó la lógica de negocio y ejecutó `basicAck`. El mensaje fue eliminado de la cola."

#### Paso 3: Demostrar Flujo de Fallo con NACK → DLQ (3 min)

```bash
# Verificar estado inicial de la DLQ (debería tener 0 mensajes)
curl -s -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .

# Publicar mensaje con fallo simulado (prefijo FAIL-)
curl -X POST http://localhost:8080/api/admin/amqp/test-publish \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "exchange": "vidasalud.direct.exchange",
    "routingKey": "citas.crear",
    "payload": {
      "eventId": "evt-fail-001",
      "appointmentId": "FAIL-APT-999",
      "patientName": "Test Fallido",
      "notes": "Este mensaje será rechazado y enviado a DLQ"
    }
  }'

# Esperar 2 segundos para que el consumidor procese
sleep 2

# Verificar que el mensaje llegó a la DLQ
curl -s -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**Verificar en logs del BFF**:
```bash
docker logs vidasalud-bff --tail 20 | grep -E "NACK|DLQ|FAIL-"
```

**Explicar**: "El consumidor detectó el patrón `FAIL-` en el appointmentId, lanzó una excepción de validación y ejecutó `basicNack(deliveryTag, false, false)`. RabbitMQ automáticamente redirigió el mensaje al exchange DLX, que lo depositó en `citas.agendamiento.dlq`."

#### Paso 4: Demostrar Seguridad de la API Admin (2 min)

```bash
# Intento SIN token → 401 Unauthorized
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue

# Intento con token de Paciente → 403 Forbidden
export PACIENTE_TOKEN="<token-con-rol-paciente>"
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue \
  -H "Authorization: Bearer $PACIENTE_TOKEN"

# Intento con token de Admin → 200 OK
curl -s -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**Explicar**: "La API administrativa está protegida con `@PreAuthorize("hasRole('Admin')")`. Solo usuarios con el rol Admin en Microsoft Entra ID pueden gestionar RabbitMQ."

#### Paso 5: Limpiar DLQ (1 min)
```bash
# Purgar la DLQ de pruebas
curl -X POST http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq/purge \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .

# Verificar que quedó vacía
curl -s -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.dlq \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .messageCount
```

### 10.3 Puntos Clave para la Presentación

1. **Arquitectura Event-Driven**: Los productores publican eventos de negocio (citas, auditoría) de forma asíncrona sin bloquear el flujo HTTP.

2. **Confirmación Manual ACK/NACK**: Garantiza que ningún mensaje se pierde. Solo se elimina de la cola cuando el procesamiento es exitoso.

3. **Dead Letter Queues**: Los mensajes fallidos no se descartan ni bloquean la cola principal. Se almacenan en DLQs para inspección, auditoría y reintento manual.

4. **Seguridad Integrada**: La API administrativa hereda la autenticación OAuth2 de Microsoft Entra ID con control de acceso basado en roles.

5. **Observabilidad**: Logs estructurados con traceId, deliveryTag y estado de ACK/NACK para debugging y auditoría.

### 10.4 Preguntas Frecuentes del Evaluador

| Pregunta | Respuesta |
|---|---|
| ¿Por qué usar ACK manual en lugar de automático? | ACK automático confirma el mensaje antes de procesarlo, lo que puede causar pérdida de datos si el consumidor falla. ACK manual garantiza confirmación solo tras procesamiento exitoso. |
| ¿Qué pasa si RabbitMQ se cae? | El BFF tiene retry configurado (3 intentos, backoff exponencial). Si no hay broker, las publicaciones fallan gracefully y se loggean. El BFF continúa sirviendo otros endpoints. |
| ¿Por qué `requeue=false` en NACK? | Con `requeue=true`, el mensaje volvería a la cola y se reprocesaría infinitamente (loop). Con `requeue=false`, va directamente a la DLQ para inspección manual. |
| ¿Cómo escalaría esto en producción? | Se usa un cluster de 3+ nodos con Quorum Queues para replicación, HAProxy/ALB para balanceo, y múltiples instancias del BFF consumiendo de las mismas colas. |

---

## 11. Despliegue en AWS EC2 (Procedimiento Seguro)

### 11.1 Pre-requisitos en EC2
- Instancia existente con Docker instalado
- Red Docker `vidasalud_default` ya creada
- BFF corriendo como contenedor `vidasalud-bff`
- Backup de configuración existente

### 11.2 Procedimiento de Despliegue

> [!CAUTION]
> NO ejecutar estos comandos sin autorización explícita. Este procedimiento modifica infraestructura en producción.

#### Fase 1: Verificación Pre-Despliegue
```bash
# Conectar vía SSM
aws ssm start-session --target i-0dff7eb1e4dcd94d0

# Verificar recursos disponibles
free -m
df -h
docker ps
docker network ls | grep vidasalud
```

#### Fase 2: Desplegar RabbitMQ
```bash
# Cargar variables de entorno
source /root/vidasalud-backup/rabbitmq.env

# Ejecutar RabbitMQ en la red existente
docker run -d \
  --name vidasalud-rabbitmq \
  --hostname rabbitmq \
  --network vidasalud_default \
  --restart unless-stopped \
  --memory 200m \
  -e RABBITMQ_DEFAULT_USER=${RABBITMQ_USERNAME} \
  -e RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASSWORD} \
  -e RABBITMQ_DEFAULT_VHOST=/ \
  rabbitmq:3.13-management-alpine

# Esperar healthcheck (30-60 segundos)
sleep 30
docker exec vidasalud-rabbitmq rabbitmq-diagnostics -q ping
```

#### Fase 3: Actualizar BFF con Conexión a RabbitMQ
```bash
# Detener BFF actual
docker stop vidasalud-bff

# Respaldar configuración actual
docker inspect vidasalud-bff > /root/vidasalud-backup/bff-inspect-$(date +%Y%m%d).json

# Eliminar contenedor (no la imagen)
docker rm vidasalud-bff

# Recrear BFF con variables RabbitMQ
docker run -d \
  --name vidasalud-bff \
  --network vidasalud_default \
  --restart unless-stopped \
  -p 8080:8080 \
  -e AZURE_TENANT_ID=${AZURE_TENANT_ID} \
  -e AZURE_API_CLIENT_ID=${AZURE_API_CLIENT_ID} \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_RABBITMQ_HOST=vidasalud-rabbitmq \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_USERNAME=${RABBITMQ_USERNAME} \
  -e SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD} \
  ms-vidasalud-bff:rabbitmq
```

#### Fase 4: Verificación Post-Despliegue
```bash
# Verificar contenedores corriendo
docker ps

# Verificar logs del BFF (conexión a RabbitMQ)
docker logs vidasalud-bff --tail 50 | grep -i rabbit

# Verificar colas declaradas
docker exec vidasalud-rabbitmq rabbitmqctl list_queues name messages

# Test de salud del BFF
curl -s http://localhost:8080/actuator/health | jq .
```

### 11.3 Procedimiento de Rollback
```bash
# 1. Detener y eliminar nuevo BFF
docker stop vidasalud-bff && docker rm vidasalud-bff

# 2. Restaurar BFF original (sin RabbitMQ)
docker run -d \
  --name vidasalud-bff \
  --network vidasalud_default \
  --restart unless-stopped \
  -p 8080:8080 \
  -e AZURE_TENANT_ID=${AZURE_TENANT_ID} \
  -e AZURE_API_CLIENT_ID=${AZURE_API_CLIENT_ID} \
  -e SPRING_PROFILES_ACTIVE=dev \
  ms-vidasalud-bff:latest

# 3. Detener y eliminar RabbitMQ (opcional)
docker stop vidasalud-rabbitmq && docker rm vidasalud-rabbitmq
```

---

## 12. Resumen de Implementación

| Componente | Estado | Archivos |
|---|---|---|
| Configuración RabbitMQ | ✅ Completo | `RabbitMQConfig.java`, `RabbitMQConstants.java` |
| Productores | ✅ Completo | `AppointmentEventProducer.java`, `AuditEventProducer.java` |
| Consumidores (ACK/NACK) | ✅ Completo | `AppointmentCreationConsumer.java`, `AppointmentConfirmationConsumer.java`, `NotificationAuditConsumer.java` |
| API Admin REST | ✅ Completo | `RabbitAdminController.java`, `RabbitAdminService.java` |
| Seguridad (@PreAuthorize) | ✅ Completo | Solo rol Admin puede acceder a `/api/admin/amqp/**` |
| Dead Letter Queues | ✅ Completo | 3 DLQs configuradas con bindings al DLX |
| Tests Unitarios | ✅ 34 tests | 0 fallos, 0 errores |
| Docker Compose Local | ✅ Completo | `docker-compose.yml` |
| Docker Compose Cluster | ✅ Completo | `docker-compose.cluster.yml` (2 nodos) |
| Documentación | ✅ Completo | `RABBITMQ_GUIDE.md` |
