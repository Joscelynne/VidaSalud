# VidaSalud — Evaluación Parcial N°1 (DSY1107 Cloud Native I)

Plataforma empresarial de gestión de atenciones médicas construida con arquitectura **Cloud Native**: **React 19 (Vite + TypeScript + MSAL)** para el Frontend y **Spring Boot 3.5.x (Java 21 + Spring Security)** para el Backend For Frontend (BFF), integrados mediante autenticación **JWT** con **Azure AD (Microsoft Entra ID)**.

---

## 🏛️ Arquitectura Obligatoria

```
[ Usuario / Navegador ]
        │
        ▼ (Login Redirect / Silent Token)
[ Azure AD / Microsoft Entra ID ]
        │
        ▼ (Access Token JWT Bearer)
[ React 19 SPA (Vite + MSAL) ] ── (Axios Interceptor) ──► [ AWS API Gateway ]
                                                                 │
                                                                 ▼
                                                [ Spring Boot 3.5 BFF (Java 21) ]
                                                (OAuth2 Resource Server & JWT)
                                                                 │
                                                                 ▼
                                                [ Microservicios VidaSalud ]
```

---

## 🚀 Tecnologías Utilizadas

### Frontend
- **React 19** + **Vite 6** + **TypeScript**.
- **MSAL React** (`@azure/msal-react` v3 y `@azure/msal-browser` v5).
- **React Router DOM v7**.
- **Axios** (Interceptor automático de Tokens JWT Bearer y renovación silenciosa).
- **Context API** (Sin Redux).
- **Tailwind CSS v4** + **Lucide Icons**.

### Backend BFF (`ms-vidasalud-bff`)
- **Java 21 LTS**.
- **Spring Boot 3.4.3** (Java 21 + Spring Security 6).
- **OAuth2 Resource Server JWT** (Microsoft Entra ID).
- **RabbitMQ 3.13** (Broker AMQP con Management Plugin).
- **Spring AMQP** (Topología de 6 colas durables, DLQ, ACK/NACK manual y API administrativa).
- **Maven**.
- **Lombok** & **Spring Boot Actuator**.
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
VITE_AZURE_CLIENT_ID=<YOUR_AZURE_CLIENT_ID>
VITE_AZURE_TENANT_ID=<YOUR_TENANT_ID>
VITE_API_SCOPE=api://<YOUR_AZURE_CLIENT_ID>/access_as_user
VITE_API_GATEWAY_URL=http://localhost:8080

# Backend (.env)
AZURE_TENANT_ID=<YOUR_TENANT_ID>
AZURE_API_CLIENT_ID=<YOUR_AZURE_CLIENT_ID>
SPRING_PROFILES_ACTIVE=dev
```

---

## 🐳 Ejecución con Docker Compose

Para compilar e iniciar la infraestructura completa (Frontend + Backend BFF + RabbitMQ):

```bash
docker compose up --build -d
```

- **Frontend React**: `http://localhost:3000` (o `http://localhost:80`)
- **Backend Spring Boot BFF**: `http://localhost:8080`
- **RabbitMQ Management Console**: `http://localhost:15672` (Usuario: `vidasalud`, Contraseña: `vidasalud_local_2026`)
- **Endpoint de Verificación JWT**: `http://localhost:8080/api/me`

---

## 🐰 Integración RabbitMQ y Mensajería Asíncrona

Consulta la documentación técnica detallada en [`RABBITMQ_GUIDE.md`](RABBITMQ_GUIDE.md):
- **Topología**: 3 exchanges (`direct`, `topic`, `dlx`), 6 colas durables (3 principales y 3 DLQ).
- **Procesamiento Asíncrono**: Productores tipados (`AppointmentEventProducer`, `AuditEventProducer`) y consumidores con confirmación manual ACK/NACK sin requeue.
- **API REST Administrativa**: Endpoints en `/api/admin/amqp/**` protegidos exclusivamente con rol `Admin`.
- **Pruebas Automatizadas**: Suite completa de tests unitarios, de seguridad y prueba de integración de flujo DLQ.
- **Preparación Cluster EP4**: Configuración de cluster de 2 nodos en [`docker-compose.cluster.yml`](docker-compose.cluster.yml).

---

## 🧪 Pruebas de Seguridad y Rúbrica DSY1107

Revisa el archivo [`SECURITY_TESTS.md`](SECURITY_TESTS.md) para ejecutar las pruebas requeridas:
1. **Redirección de Rutas Protegidas en React** (`/login`).
2. **Denegación por Rol en Frontend** (`/unauthorized`).
3. **Respuesta 401 Unauthorized JSON** (Sin Token o Token Expirado).
4. **Respuesta 403 Forbidden JSON** (Falta de Rol en Token JWT).
5. **Respuesta 200 OK con Claims** (Endpoint `/api/me`).
6. **Protección Rol Admin en API Administrativa RabbitMQ** (`/api/admin/amqp/**`).
