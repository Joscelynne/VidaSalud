# Guía de Integración Apache Kafka — VidaSalud (EP5 / EP6)

Documentación técnica integral sobre la arquitectura de **Streaming y Analítica en Tiempo Real**, topología de clúster con alta disponibilidad (3 nodos ZooKeeper + 3 brokers Kafka), diseño de tópicos, DTOs sin datos sensibles (PII), Consumer Groups, gestión de offsets, control de lag, API administrativa REST, panel Kafka UI y guión de demostración para la **Evaluación Parcial N°6 (EP6)**.

---

## 1. Arquitectura y Convivencia: Kafka vs RabbitMQ

En la plataforma **VidaSalud**, ambos sistemas de mensajería coexisten armónicamente resolviendo requerimientos distintos y complementarios:

```
                               [ Usuario / Recepcionista / Paciente ]
                                                 │
                                                 ▼
                                   [ Frontend React 19 SPA ]
                                                 │ (Bearer JWT MSAL)
                                                 ▼
                                [ Backend BFF (Spring Boot 3.4.3) ]
                                  │                             │
        ┌─────────────────────────┘                             └─────────────────────────┐
        ▼                                                                                 ▼
 🐰 RABBITMQ 3.13 (Broker Transaccional)                         🌀 APACHE KAFKA (Streaming & Analítica)
 ──────────────────────────────────────────                      ───────────────────────────────────────
 • Modelo: Message Broker tradicional                           • Modelo: Distributed Commit Log inmutable
 • Patrón: Task Distribution, RPC asíncrono                      • Patrón: Event Streaming, telemetría y KPIs
 • Semántica: ACK/NACK manual por mensaje                        • Semántica: Offsets y Consumer Groups desacoplados
 • Ciclo de vida: Los mensajes se consumen y borran              • Ciclo de vida: Retención por tiempo (3 a 7 días)
 • Casos de uso VidaSalud:                                       • Casos de uso VidaSalud:
   - Agendamiento transaccional (citas.crear)                      - Streaming de atenciones (vidasalud.atenciones.stream)
   - Confirmación de box (citas.confirmar)                         - Telemetría y alertas (vidasalud.telemetria.stream)
   - Notificaciones y DLQ por fallo                                - Monitoreo de lag, offsets y analítica en Kafka UI
```

> **Principio de Aislamiento:** Las operaciones de Kafka se ejecutan de manera desacoplada y protegida (`try/catch` con fallback operacional). Una falla o mantenimiento en el clúster Kafka jamás aborta ni revierte las transacciones médicas ni las colas de RabbitMQ.

---

## 2. Discrepancias Arquitecturales y Justificación para el Docente

| Aspecto de la Pauta EP5 / EP6 | Implementación Real VidaSalud | Justificación Técnica y Arquitectural |
|---|---|---|
| **Estructura de Backend:** La pauta menciona "varios microservicios en Java/Spring Boot". | **Backend For Frontend (BFF) Modular (`ms-vidasalud-bff`)**: Monolito modular en Spring Boot 3.4.3 / Java 21 con capas delimitadas (Controladores, Servicios, Productores, Consumidores, Seguridad). | El patrón BFF centraliza la agregación de llamadas, la validación JWT contra Entra ID y el gobierno de colas y tópicos. Dentro del BFF, los módulos de RabbitMQ y Kafka operan como dominios desacoplados con hilos y contenedores independientes. |
| **Frontend:** La pauta menciona "componente Angular". | **Frontend React 19 SPA (Vite + TypeScript + MSAL + Tailwind CSS v4)**. | React 19 proporciona soporte nativo de concurrencia, integración oficial con Azure MSAL (`@azure/msal-react` v5), vistas operacionales y paneles de administración dedicados tanto para RabbitMQ como para Kafka (`/admin/kafka`). |
| **Gestión de Identidad (IDaaS) y API Manager:** Se mencionan componentes de nube (AWS API Gateway, IDaaS). | Se implementa soporte completo de tokens Bearer JWT validados contra **Microsoft Entra ID (Azure AD)** en los filtros de Spring Security, con endpoints locales y simulados para pruebas locales reproducibles sin costo. | Permite validar la pauta en entornos locales y en AWS indistintamente. |

---

## 3. Arquitectura del Clúster Distribuido (Docker Compose)

La infraestructura de streaming se despliega mediante [`docker-compose.kafka.yml`](docker-compose.kafka.yml), configurando tolerancia a fallos estricta:

```
                           [ Kafka UI (Puerto 8090) ]
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            ▼                          ▼                          ▼
   [ Kafka Broker 1 ]         [ Kafka Broker 2 ]         [ Kafka Broker 3 ]
    (ID: 1 | Port: 9092)       (ID: 2 | Port: 9093)       (ID: 3 | Port: 9094)
            │                          │                          │
            └──────────────────────────┼──────────────────────────┘
                                       │
            ┌──────────────────────────┼──────────────────────────┐
            ▼                          ▼                          ▼
     [ ZooKeeper 1 ]            [ ZooKeeper 2 ]            [ ZooKeeper 3 ]
       (Container: 2181)          (Container: 2181)          (Container: 2181)
       (Peers: 2888/3888)         (Peers: 2888/3888)         (Peers: 2888/3888)
```

### 3.1 Justificación de los 3 Nodos ZooKeeper (Quórum 2f+1)
- **Rol:** ZooKeeper coordina el clúster Kafka, mantiene el registro de brokers activos, lidera la elección del Controller de Kafka y almacena metadatos de particiones.
- **Tolerancia a fallos:** El algoritmo de consenso de ZooKeeper requiere quórum de mayoría simple ($Q = \lfloor N/2 \rfloor + 1$). Con 3 nodos ($N=3$), el quórum es 2. Si 1 nodo ZooKeeper se detiene o falla, los 2 nodos restantes mantienen el quórum intacto ($2 \ge 2$), garantizando disponibilidad ininterrumpida.

### 3.2 Justificación de los 3 Brokers Kafka (Réplica 3 y Particiones 3)
- **Rol:** Almacenan los registros distribuidos y atienden lecturas y escrituras de productores y consumidores.
- **Factor de Replicación 3 (`replication.factor=3`):** Cada partición de un tópico posee 1 réplica líder (Leader) y 2 réplicas seguidoras (Followers) distribuidas equitativamente entre los 3 brokers.
- **Mínimo de Réplicas en Sincronía (`min.insync.replicas=2`):** Junto con `acks=all`, el productor solo considera confirmado un evento cuando al menos 2 réplicas han persistido el registro en disco. Esto permite que el sistema tolere la pérdida de 1 broker completo sin riesgo de pérdida de datos.

---

## 4. Diseño de Tópicos y Eventos (Sin Datos Sensibles / No PII)

El evento analítico minimiza los datos y no publica nombres, RUT, identificadores de pacientes o citas, códigos de profesionales ni box. La especialidad y el estado aún pueden revelar contexto clínico, por lo que el acceso al tópico debe seguir restringido.

### 4.1 Tópico 1: `vidasalud.atenciones.stream`
- **Propósito:** Streaming de analítica sobre el ciclo de vida de las atenciones para monitoreo de ocupación de boxes, tiempos de consulta y demanda por especialidad.
- **Configuración:**
  - **Particiones:** 3
  - **Factor de Réplica:** 3
  - **Retención:** 7 días (`retention.ms: 604800000`)
  - **Cleanup Policy:** `delete`
  - **Clave de Particionado:** `specialty` (mantiene el orden por especialidad; el hash de las claves determina su partición, por lo que el balance depende de la variedad y frecuencia de las especialidades).

**Estructura del Evento (`AtencionMetricaEvent`):**
```json
{
  "eventId": "evt-stream-4f89b210",
  "specialty": "Cardiología",
  "status": "CONFIRMED",
  "timestamp": "2026-11-17T18:30:00Z",
  "traceId": "trc-89a1bc23"
}
```

### 4.2 Tópico 2: `vidasalud.telemetria.stream`
- **Propósito:** Streaming de observabilidad técnica en tiempo real, latencias de peticiones, códigos HTTP de respuesta y detección automática de anomalías operacionales.
- **Configuración:**
  - **Particiones:** 3
  - **Factor de Réplica:** 3
  - **Retención:** 3 días (`retention.ms: 259200000`)
  - **Cleanup Policy:** `delete`
  - **Clave de Particionado:** `sourceComponent` (ordena y agrupa métricas por módulo emisor).

**Estructura del Evento (`TelemetriaClinicaEvent`):**
```json
{
  "telemetryId": "tel-sim-91b3fa12",
  "sourceComponent": "ms-vidasalud-bff",
  "operationType": "CREATE_APPOINTMENT",
  "executionTimeMs": 145,
  "statusCode": 201,
  "serverNode": "node-bff-1",
  "severity": "INFO",
  "systemLoadPct": 28.4,
  "timestamp": "2026-11-17T18:30:05Z",
  "traceId": "trc-11c2da45",
  "details": "Atención médica procesada satisfactoriamente"
}
```

---

## 5. Consumer Groups, Offsets y Lag

En Kafka, cada Consumer Group representa un suscriptor lógico independiente que gestiona sus propios offsets de lectura:

| Consumer Group | Tópico Suscrito | Responsabilidad de Negocio | Política ante Fallos |
|---|---|---|---|
| `vidasalud-analitica-group` | `vidasalud.atenciones.stream` | Calcula métricas en tiempo real: volumen de consultas por especialidad, tiempos promedio y estados de atención. | Validación estricta con `AtencionMetricaConsumer`; 3 reintentos con `FixedBackOff(1000ms)`. |
| `vidasalud-alertas-group` | `vidasalud.telemetria.stream` | Monitorea eventos de telemetría y detecta anomalías: genera alertas operacionales cuando la latencia supera los 2000 ms o el status es 5xx. | Clasificación por severidad en `TelemetriaClinicaConsumer`. |

### Conceptos Clave Evaluados en EP6:
- **Current Offset:** Último mensaje consumido y confirmado por el grupo en una partición específica.
- **Log End Offset (LEO):** Último mensaje escrito y disponible en la partición por los productores.
- **Consumer Lag:** Diferencia entre el Log End Offset y el Current Offset ($\text{Lag} = \text{LEO} - \text{Current Offset}$). Si el Lag crece continuamente, indica que el consumidor está procesando más lento de lo que los productores emiten, justificando escalar consumidores asignados a las particiones disponibles.

---

## 6. API REST Administrativa de Kafka (`/api/admin/kafka/**`)

Endpoints protegidos exclusivamente para usuarios con rol `Admin`:

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/admin/kafka/topics` | Lista todos los tópicos con particiones, factor de réplica, retención y cleanup. |
| `GET` | `/api/admin/kafka/topics/{topicName}` | Detalle exhaustivo de un tópico (particiones, líder, réplicas, ISRs y configs). |
| `POST` | `/api/admin/kafka/topics` | Crea un nuevo tópico con validación de nombre, particiones y réplicas. |
| `PATCH` | `/api/admin/kafka/topics/{topicName}/config` | Modifica dinámicamente configuraciones del tópico (ej. `retention.ms`). |
| `DELETE` | `/api/admin/kafka/topics/{topicName}` | Elimina un tópico (protege los tópicos esenciales del sistema). |
| `GET` | `/api/admin/kafka/consumer-groups` | Inspecciona todos los consumer groups activos, estado, asignación y lag. |
| `POST` | `/api/admin/kafka/publish/test` | Genera y publica eventos de prueba simulados (`ATENCIONES` o `TELEMETRIA`). |

---

## 7. Instrucciones de Ejecución y Conectividad

### 7.1 Iniciar el Clúster Kafka (ZooKeeper 3 nodos + Kafka 3 brokers + Kafka UI)

```bash
docker compose -f docker-compose.kafka.yml up -d
```

> **Variable de entorno `KAFKA_ADVERTISED_HOST`:**
> Los brokers configuran dos listeners:
> - `INTERNAL` (puertos 29092, 29093, 29094): para comunicación interna entre brokers y con `kafka-ui` dentro de la red Docker (`vidasalud-net`).
> - `EXTERNAL` (puertos 9092, 9093, 9094): expuestos hacia el host para clientes externos.
>
> Por defecto, el host anunciado en el listener externo es `172.31.67.180` (IP privada configurada para el host Kafka en AWS EC2).
> Para levantar y probar el clúster localmente en tu máquina (Mac/Linux/Windows), define la variable `KAFKA_ADVERTISED_HOST=localhost`:
> ```bash
> KAFKA_ADVERTISED_HOST=localhost docker compose -f docker-compose.kafka.yml up -d
> ```

Verificar que los servicios de infraestructura estén activos:
```bash
docker compose -f docker-compose.kafka.yml ps
```

Acceder a las interfaces:
- **Kafka UI:** `http://localhost:8090` (conectado a los listeners internos `kafka1:29092,kafka2:29093,kafka3:29094`)
- **BFF Spring Boot:** `http://localhost:8080`
- **Frontend React:** `http://localhost:3000` (Panel: `/admin/kafka`)

### 7.2 Conexión del BFF según el Entorno de Ejecución

El BFF se comunica con Kafka a través de `SPRING_KAFKA_BOOTSTRAP_SERVERS`. La dirección a utilizar depende de dónde se ejecute el BFF:

1. **BFF Local fuera de Docker (ejecutado con Maven/IDE en tu máquina):**
   - Requiere haber levantado Kafka con `KAFKA_ADVERTISED_HOST=localhost`.
   - Conexión a través de los puertos externos mapeados al host (9092, 9093, 9094):
   ```bash
   cd ms-vidasalud-bff
   SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094 mvn spring-boot:run
   ```

2. **BFF dentro de Docker en la misma red (`vidasalud-net`):**
   - Si el BFF se ejecuta en un contenedor Docker en la misma máquina o red que Kafka:
   ```
   SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka1:29092,kafka2:29093,kafka3:29094
   ```

3. **BFF en otra máquina / instancia EC2 (ej. `172.31.89.28` conectando a `172.31.67.180`):**
   - Utiliza la IP privada del host Kafka y los puertos externos (9092, 9093, 9094):
   ```
   SPRING_KAFKA_BOOTSTRAP_SERVERS=172.31.67.180:9092,172.31.67.180:9093,172.31.67.180:9094
   ```
   - *Consideración de red:* Esta configuración asume que ambas instancias están en la misma VPC (o interconectadas) y que el Security Group de AWS en la instancia de Kafka permite tráfico entrante TCP en los puertos 9092, 9093 y 9094 desde la IP del BFF. *(Nota: La operatividad efectiva de la infraestructura en AWS no se da por asumida y debe verificarse directamente en la consola de AWS o mediante comprobación de conectividad antes de la presentación)*.

---

## 8. Guión de Demostración Paso a Paso para la Evaluación Parcial N°6 (EP6)

Sigue este guión durante la defensa oral ante el docente para cubrir el 100% de la rúbrica:

### Paso 1: Demostrar Arquitectura en Docker Compose (10% Rúbrica)
1. Ejecutar en terminal:
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
   ```
2. **Explicación oral:**
   > *"Profesor, aquí observamos el ensamble de 3 nodos ZooKeeper (`vidasalud-zk-1`, `vidasalud-zk-2`, `vidasalud-zk-3`) que implementan un quórum de mayoría simple 2f+1, garantizando que el clúster tolere la caída de 1 nodo ZK sin interrupción. Además, tenemos 3 brokers Kafka independientes (`vidasalud-kafka-1`, `2` y `3`), permitiendo un factor de replicación 3 con ISR mínimo de 2."*

### Paso 2: Mostrar Tópicos, Particiones y Réplicas en Kafka UI (10% + 5% Rúbrica)
1. Abrir en el navegador: `http://localhost:8090`.
2. Ir a la pestaña **Topics**:
   - Mostrar `vidasalud.atenciones.stream`: 3 Partitions, Replication Factor 3.
   - Mostrar `vidasalud.telemetria.stream`: 3 Partitions, Replication Factor 3.
3. Hacer clic en `vidasalud.atenciones.stream` $\rightarrow$ pestaña **Overview**:
   - Mostrar que las réplicas están distribuidas entre los brokers 1, 2 y 3.
   - Mostrar que el **In-Sync Replicas (ISR)** es [1, 2, 3].
4. Ir a la pestaña **Settings**:
   - Mostrar `retention.ms = 604800000` (7 días) y `cleanup.policy = delete`.

### Paso 3: Explicar Diseño de Eventos y Privacidad No PII (6% Rúbrica)
1. Mostrar la estructura de `AtencionMetricaEvent`.
2. **Explicación oral:**
El evento no transmite datos de pacientes, citas, profesionales ni boxes. No usamos hashes simples como sustituto de anonimización.

### Paso 4: Demostrar Producción y Consumo en Tiempo Real (12% + 12% Rúbrica)
1. En el Frontend de VidaSalud, ingresar a `http://localhost:3000/admin/kafka` con usuario Administrador.
2. En la sección **Simulador de Eventos de Streaming (EP6)**:
   - Seleccionar `Atención Médica (vidasalud.atenciones.stream)`, elegir `Cardiología` y presionar **Publicar Evento a Kafka**.
   - Cambiar a `Telemetría`, ingresar latencia `2500 ms` y presionar **Publicar Evento a Kafka**.
3. En la terminal con logs del BFF:
   - Mostrar el log del productor: `[KAFKA-PRODUCER] AtencionMetricaEvent entregado con éxito a [vidasalud.atenciones.stream], partición [X], offset [Y]`.
   - Mostrar el log del consumidor: `[KAFKA-CONSUMER-ANALITICA] Evento procesado exitosamente... Total procesados: [Z]`.
   - Mostrar la alerta por latencia: `[KAFKA-ALERTA-OPERACIONAL] Anomalía detectada... latencia [2500 ms]`.

### Paso 5: Inspeccionar Consumer Groups, Offsets y Lag (15% Rúbrica)
1. En Kafka UI, ir a la pestaña **Consumers**:
   - Mostrar el grupo `vidasalud-analitica-group` y `vidasalud-alertas-group`.
   - Hacer clic en `vidasalud-analitica-group` y mostrar la tabla de particiones (0, 1, 2), el **Current Offset**, el **End Offset** y el **Lag** (0 si está al día).
2. Alternativamente, ejecutar el script administrativo en terminal:
   ```bash
   ./scripts/kafka-admin-cli.sh describe
   ```

### Paso 6: Demostrar Tolerancia a Fallos (Resiliencia ante Caída de Broker)
1. En vivo, detener 1 broker:
   ```bash
   docker stop vidasalud-kafka-3
   ```
2. Refrescar Kafka UI:
   - Los brokers activos pasarán a ser 2 de 3.
   - El ISR de las particiones pasará a ser [1, 2] (cumpliendo `min.insync.replicas=2`).
3. Publicar un nuevo evento desde el Frontend o cURL:
   - El evento se publicará y consumirá sin interrupción.
4. Reiniciar el broker:
   ```bash
   docker start vidasalud-kafka-3
   ```
   - El broker se reincorporará automáticamente al ISR en Kafka UI.

---

## 9. Script Interactivo `scripts/kafka-admin-cli.sh`

Para operar el clúster directamente mediante comandos CLI oficiales (`kafka-topics` y `kafka-consumer-groups`):

```bash
./scripts/kafka-admin-cli.sh
```

Menú interactivo disponible:
1. Listar todos los tópicos.
2. Describir tópico y sus réplicas.
3. Crear un nuevo tópico.
4. Modificar retención (`retention.ms`).
5. Eliminar tópico no protegido.
6. Listar Consumer Groups.
7. Describir Consumer Group (Offsets y Lag).
8. Monitorear mensajes en tiempo real (Console Consumer).
9. Publicar evento interactivo (Console Producer).
