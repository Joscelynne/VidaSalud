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
- **Spring Boot 3.5.x** / 3.4.x.
- **Spring Security 6** (OAuth2 Resource Server JWT).
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
│   │   ├── config/               # SecurityConfig, JwtDecoderConfig, JwtAuthenticationConverterConfig, CorsConfig
│   │   ├── controller/           # UserController (/api/me), AppointmentController, CatalogController, ReportController, AuditController
│   │   ├── dto/                  # UserMeResponse, ErrorResponse, AppointmentDto, CatalogDto, ReportDto, AuditDto
│   │   ├── security/             # CustomAuthenticationEntryPoint (401 JSON), CustomAccessDeniedHandler (403 JSON)
│   │   └── filter/               # RequestLoggingFilter (OncePerRequestFilter con TraceId y logs auditables)
│   ├── src/main/resources/
│   │   ├── application.yml       # Configuración con issuer-uri y audiences de Azure AD
│   │   └── application-dev.yml
│   ├── .env.example              # Plantilla de variables de entorno Backend
│   ├── pom.xml                   # Dependencias Maven oficiales
│   └── Dockerfile                # Build multi-stage: Maven + Java 21 -> JRE 21 Alpine
├── docker-compose.yml            # Orquestación de Frontend y BFF
├── SECURITY_TESTS.md             # Guía de pruebas de seguridad y casos cURL
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

Para compilar e iniciar la infraestructura completa (Frontend + Backend BFF) mediante Docker:

```bash
docker-compose up --build
```

- **Frontend React**: `http://localhost:3000` (o `http://localhost:80`)
- **Backend Spring Boot BFF**: `http://localhost:8080`
- **Endpoint de Verificación JWT**: `http://localhost:8080/api/me`

---

## 🧪 Pruebas de Seguridad y Rúbrica DSY1107

Revisa el archivo [`SECURITY_TESTS.md`](SECURITY_TESTS.md) para ejecutar las pruebas requeridas:
1. **Redirección de Rutas Protegidas en React** (`/login`).
2. **Denegación por Rol en Frontend** (`/unauthorized`).
3. **Respuesta 401 Unauthorized JSON** (Sin Token o Token Expirado).
4. **Respuesta 403 Forbidden JSON** (Falta de Rol en Token JWT).
5. **Respuesta 200 OK con Claims** (Endpoint `/api/me`).
