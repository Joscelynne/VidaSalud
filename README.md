# VidaSalud — Evaluación Parcial N°1 (DSY1107 Cloud Native I)

Plataforma empresarial de gestión de atenciones médicas construida con arquitectura **Cloud Native**: **React 19 (Vite + TypeScript + MSAL)** para el Frontend y **Spring Boot 3.4.3 (Java 21 + Spring Security)** para el Backend For Frontend (BFF), integrados mediante autenticación **JWT** con **Azure AD (Microsoft Entra ID)**.

---

## 📋 Resumen Ejecutivo (Verificado)

Este resumen refleja exclusivamente las características, estado y componentes confirmados en el repositorio y en el flujo de GitHub Actions:

- **Arquitectura y Stack**:
  - **Frontend SPA**: React 19 (`^19.2.8`), TypeScript (`~6.0.2`), Vite (`^8.2.2`), Tailwind CSS v4 (`^4.3.3`), `@azure/msal-react` (`^5.7.0`), `@azure/msal-browser` (`^5.21.0`), React Router v7 (`^7.18.3`) y Axios con interceptor Bearer JWT.
  - **Backend BFF (`ms-vidasalud-bff`)**: Java 21 LTS, Spring Boot 3.4.3, Spring Security 6 (OAuth2 Resource Server con validación JWT), Spring Boot Actuator (`/actuator/health`), Lombok y Maven.
  - **Persistencia**: PostgreSQL con acceso a datos mediante `JdbcTemplate` (`spring-boot-starter-jdbc`, sin Spring Data JPA / Hibernate) y migraciones de base de datos automatizadas con **Flyway** (`V1__create_appointment_table.sql`, `V2__migrate_existing_appointments.sql`, `V3__create_notifications_table.sql`).
  - **Mensajería Asíncrona (RabbitMQ 3.13)**: Topología de 3 exchanges durables (`vidasalud.direct.exchange`, `vidasalud.topic.exchange`, `vidasalud.dlx.exchange`), 3 colas principales durables con configuración Dead Letter (`citas.agendamiento.queue`, `citas.confirmacion.queue`, `notificaciones.auditoria.queue`) y 3 Dead Letter Queues (`citas.agendamiento.dlq`, `citas.confirmacion.dlq`, `notificaciones.auditoria.dlq`).
  - **Confirmación AMQP**: Contenedor configurado con `AcknowledgeMode.MANUAL` y `defaultRequeueRejected(false)`. Los consumidores ejecutan `channel.basicAck` ante éxito y `channel.basicNack(requeue=false)` ante errores de validación no recuperables, enrutando los mensajes fallidos a su DLQ.
  - **Seguridad y RBAC**: Roles confirmados en el BFF: `Admin`, `Recepcionista`, `Paciente`, `Auditor`. Respuestas JSON estandarizadas ante fallos: `401 Unauthorized` (`CustomAuthenticationEntryPoint`) y `403 Forbidden` (`CustomAccessDeniedHandler`). Módulo de administración AMQP (`/api/admin/amqp/**`) restringido exclusivamente a `Admin`.

- **Pipeline CI/CD y Despliegue en AWS (`.github/workflows/bff-ci-cd.yml`)**:
  - **Pruebas en CI**: El job `test` se ejecuta en `ubuntu-latest` mediante `mvn -B -f ms-vidasalud-bff/pom.xml test '-Dtest=!*IntegrationTest'`. Ejecuta pruebas unitarias y de seguridad; las clases `*IntegrationTest` (que requieren Docker/Testcontainers) se excluyen de este pipeline.
  - **Runner Autohospedado en EC2**: El job `deploy` utiliza un runner autohospedado en Linux x64 con la etiqueta `vidasalud-ec2`. El hecho de que el runner figure como "Online" en GitHub solo valida su conexión como daemon de escucha; no acredita que un despliegue haya sido ejecutado con éxito.
  - **Condiciones de Despliegue**: El job de despliegue se omite por completo en Pull Requests. Solo se dispara ante eventos `push` a la rama `main` que modifiquen la ruta `ms-vidasalud-bff/**` (excluyendo `.dockerignore`), requiriendo la aprobación previa y obligatoria del job `test`.

- **Estado Actual de la Rama y del Flujo de Creación de Citas**:
  - **Rama Activa**: `feature/agendamiento-notificaciones-rabbitmq`. Aunque la rama ha incorporado merges recientes desde `main`, sus propios desarrollos (notificaciones, Flyway V3, controladores de citas y frontend asociado) **no están integrados en `main`**.
  - **Flujo de Creación de Citas**: Debido a errores observados en la creación de citas y a la falta de validación del endpoint en ejecución junto a un despliegue exitoso, el flujo de agendamiento **no se considera completo ni confirmado en AWS**.

---

## 🏛️ Arquitectura Obligatoria

```
[ Usuario / Navegador ]
        │
        ▼ (Login Redirect / Silent Token)
[ Azure AD / Microsoft Entra ID ]
        │
        ▼ (Access Token JWT Bearer)
[ React 19 SPA (Vite + MSAL) ] ── (Axios + JWT) ──► [ AWS API Gateway ]
                                                            │
                                                            ▼
                                           [ Spring Boot 3.4.3 BFF (Java 21) ]
                                           (OAuth2 Resource Server & JWT)
                                                │                 │
                                  (JdbcTemplate) │                 │ (Spring AMQP)
                                                ▼                 ▼
                                      [ PostgreSQL / RDS ]   [ RabbitMQ ]
```

---

## 🚀 Tecnologías Utilizadas

### Frontend
- **React 19** (`^19.2.8`) + **Vite** (`^8.2.2`) + **TypeScript** (`~6.0.2`).
- **MSAL React** (`@azure/msal-react` `^5.7.0` y `@azure/msal-browser` `^5.21.0`).
- **React Router DOM v7** (`^7.18.3`).
- **Axios** (`^1.20.0`, interceptor automático de Tokens JWT Bearer y renovación silenciosa).
- **Context API** (Sin Redux).
- **Tailwind CSS v4** (`^4.3.3`) + **Lucide Icons** (`^1.43.0`).

### Backend BFF (`ms-vidasalud-bff`)
- **Java 21 LTS**.
- **Spring Boot 3.4.3** (Java 21 + Spring Security 6).
- **OAuth2 Resource Server JWT** (Microsoft Entra ID, validación de roles y emisor).
- **Persistencia**: PostgreSQL con `JdbcTemplate` (`spring-boot-starter-jdbc`) y migraciones automáticas con **Flyway** (`V1` a `V3`).
- **RabbitMQ 3.13** (Broker AMQP con Management Plugin).
- **Spring AMQP** (Topología de 3 exchanges, 6 colas durables incluyendo 3 DLQ, ACK/NACK manual y API administrativa).
- **Maven**.
- **Lombok** & **Spring Boot Actuator** (`/actuator/health`).
- **Docker** & **Docker Compose**.

---

## 📂 Estructura del Proyecto

```
Cloud Native I/
├── frontend/                     # Aplicación React 19 + MSAL
│   ├── src/
│   │   ├── app/router/           # Router centralizado con ProtectedRoute y RoleGuard
│   │   ├── auth/                 # msalConfig.ts & authConfig.ts
│   │   ├── contexts/             # AuthContext y AuthProvider (MSAL integration)
│   │   ├── guards/               # ProtectedRoute & RoleGuard (Admin, Operator, Client, Auditor)
│   │   ├── hooks/                # useAuth, useRoles, useAccessToken
│   │   ├── services/             # Interceptor api.ts y servicios REST (appointment, catalog, report, audit)
│   │   ├── pages/                # Login, Dashboard, Appointments, Catalog, Reports, Audit, Unauthorized
│   │   ├── components/           # Navbar, Sidebar, RoleBadge y Dashboards dinámicos por rol
│   │   ├── layouts/              # MainLayout con barra lateral y superior
│   │   ├── types/                # Interfaces TypeScript estrictas
│   │   ├── utils/                # jwt.ts (decodificador y extractor de claims JWT)
│   │   └── config/               # Lectura centralizada de variables de entorno ENV
│   ├── .env.example              # Plantilla de variables de entorno Frontend
│   ├── Dockerfile                # Build multi-stage: Node 24 -> Nginx Alpine
│   └── nginx.conf                # Servidor SPA Nginx con fallback index.html
├── ms-vidasalud-bff/             # Microservicio Spring Boot BFF
│   ├── src/main/java/cl/duoc/vidasalud/bff/
│   │   ├── config/               # SecurityConfig, RabbitMQConfig, RabbitMQConstants, JwtDecoderConfig
│   │   ├── controller/           # RabbitAdminController, AppointmentController, CatalogController, ReportController, AuditController, UserController
│   │   ├── event/                # AppointmentCreatedEvent, AppointmentConfirmedEvent, NotificationAuditEvent
│   │   ├── producer/             # AppointmentEventProducer, AuditEventProducer
│   │   ├── consumer/             # AppointmentCreationConsumer, AppointmentConfirmationConsumer, NotificationAuditConsumer
│   │   ├── service/              # RabbitAdminService (gestión oficial Spring AMQP)
│   │   ├── dto/                  # DTOs REST y amqp (CreateQueueRequest, QueueStatsResponse, etc.)
│   │   ├── security/             # CustomAuthenticationEntryPoint (401 JSON), CustomAccessDeniedHandler (403 JSON)
│   │   └── filter/               # RequestLoggingFilter (OncePerRequestFilter con TraceId y logs auditables)
│   ├── src/main/resources/
│   │   ├── application.yml       # Configuración con RabbitMQ, Azure AD, Actuator y Logging
│   │   └── application-dev.yml
│   ├── .env.example              # Plantilla de variables de entorno Backend
│   ├── pom.xml                   # Dependencias Maven oficiales (incluye spring-boot-starter-amqp)
│   └── Dockerfile                # Build multi-stage: Maven + Java 21 -> JRE 21 Alpine
├── docker-compose.yml            # Orquestación Frontend, BFF y RabbitMQ 3.13 con healthcheck
├── docker-compose.cluster.yml    # Preparación de cluster RabbitMQ de 2 nodos para EP4
├── RABBITMQ_GUIDE.md             # Guía técnica completa de RabbitMQ, topología, ACK/NACK, DLQ y API REST
├── SECURITY_TESTS.md             # Guía de pruebas de seguridad, casos JWT y autorización Admin en RabbitMQ
└── README.md                     # Manual técnico y guía de ejecución
```

---

## ⚙️ Configuración de Azure AD (Microsoft Entra ID)

1. En Azure Portal, registra una aplicación de tipo **Single-Page Application (SPA)** para el Frontend.
2. Registra una aplicación **Web API** para el Backend BFF.
3. Copia las credenciales en los archivos `.env`:

```env
# Frontend (.env)
VITE_AZURE_CLIENT_ID=<SPA_CLIENT_ID>
VITE_AZURE_TENANT_ID=<YOUR_TENANT_ID>
VITE_API_SCOPE=api://<BACKEND_API_CLIENT_ID>/access_as_user
VITE_API_GATEWAY_URL=http://localhost:8080

# Backend (.env)
AZURE_TENANT_ID=<YOUR_TENANT_ID>
AZURE_API_CLIENT_ID=<BACKEND_API_CLIENT_ID>
SPRING_PROFILES_ACTIVE=dev
```

El `SPA_CLIENT_ID` identifica la aplicación frontend. El `BACKEND_API_CLIENT_ID` identifica la API registrada y debe coincidir con la audiencia que valida el BFF. La URL `http://localhost:8080` es para desarrollo local; en AWS debe configurarse la URL pública correspondiente.

---

## 🐳 Ejecución con Docker Compose

Para compilar e iniciar la infraestructura completa (Frontend + Backend BFF + RabbitMQ):

```bash
docker compose up --build -d
```

- **Frontend React**: `http://localhost:3000` (o `http://localhost:80`)
- **Backend Spring Boot BFF**: `http://localhost:8080`
- **RabbitMQ Management Console**: `http://localhost:15672` (credenciales locales de desarrollo: usuario `vidasalud`, contraseña `vidasalud_local_2026`; no reutilizar en AWS ni en producción).
- **Endpoint de Verificación JWT**: `http://localhost:8080/api/me`

---

## 🐰 Integración RabbitMQ y Mensajería Asíncrona

Consulta la documentación técnica detallada en [`RABBITMQ_GUIDE.md`](RABBITMQ_GUIDE.md):
- **Topología**: 3 exchanges (`direct`, `topic`, `dlx`), 6 colas durables (3 principales y 3 DLQ).
- **Procesamiento Asíncrono**: Productores tipados (`AppointmentEventProducer`, `AuditEventProducer`) y consumidores con confirmación manual ACK/NACK sin requeue.
- **API REST Administrativa**: Endpoints en `/api/admin/amqp/**` protegidos exclusivamente con rol `Admin`.
- **Pruebas Automatizadas**: Tests unitarios y de seguridad ejecutados en CI (`bff-ci-cd.yml` vía `mvn test '-Dtest=!*IntegrationTest'`); pruebas de integración con contenedores (`*IntegrationTest`) ejecutables en entornos locales o con Docker activo.
- **Preparación Cluster EP4**: Configuración de cluster de 2 nodos en [`docker-compose.cluster.yml`](docker-compose.cluster.yml).

---

## 🚀 Pipeline CI/CD y Despliegue en AWS (`.github/workflows/bff-ci-cd.yml`)

1. **Pruebas en GitHub-hosted Runner (`ubuntu-latest`)**:
   - Ejecuta `mvn -B -f ms-vidasalud-bff/pom.xml test '-Dtest=!*IntegrationTest'`.
   - Se limita a pruebas unitarias y de seguridad; excluye intencionalmente `*IntegrationTest` (que requieren Docker/Testcontainers).
2. **Despliegue Continuo en AWS EC2 (`vidasalud-ec2`)**:
   - Ejecuta el script `scripts/deploy-bff-ec2-runner.sh` en un runner autohospedado etiquetado como `vidasalud-ec2`.
   - El estado "Online" del runner en GitHub Actions no implica que un despliegue haya concluido exitosamente.
   - El job de despliegue se **omite en pull requests** y **únicamente corre tras un push a `main`** que afecte `ms-vidasalud-bff/**` (excluyendo `.dockerignore`), siempre que el job de pruebas finalice sin errores.
3. **Control de Salud y Rollback configurados**:
   - El script comprueba `/actuator/health` en el nuevo contenedor y está preparado para restaurar la versión anterior (`vidasalud-bff-rollback-<sha>`) si la verificación falla. Esto describe el comportamiento implementado; no confirma por sí solo que el despliegue o el rollback se hayan ejecutado exitosamente en AWS.

---

## 🧪 Pruebas de Seguridad y Rúbrica DSY1107

Revisa el archivo [`SECURITY_TESTS.md`](SECURITY_TESTS.md) para ejecutar las pruebas requeridas:
1. **Redirección de Rutas Protegidas en React** (`/login`).
2. **Denegación por Rol en Frontend** (`/unauthorized`).
3. **Respuesta 401 Unauthorized JSON** (Sin Token o Token Expirado).
4. **Respuesta 403 Forbidden JSON** (Falta de Rol en Token JWT).
5. **Respuesta 200 OK con Claims** (Endpoint `/api/me`).
6. **Protección Rol Admin en API Administrativa RabbitMQ** (`/api/admin/amqp/**`).
