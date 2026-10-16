# Suite de Pruebas de Seguridad — EP1 VidaSalud

Documentación oficial de escenarios de prueba de seguridad y validaciones JWT para la **Evaluación Parcial N°1 (DSY1107 - Duoc UC)**.

---

## 1. Pruebas de Seguridad en Frontend (React + MSAL Guards)

### Escenario F1: Acceso Anónimo a Rutas Protegidas (`ProtectedRoute`)
- **Acción**: Un usuario no autenticado abre el navegador e ingresa directamente a `http://localhost:3000/dashboard` o `/appointments`.
- **Comportamiento Esperado**:
  - `ProtectedRoute` intercepta el renderizado.
  - Se detecta `isAuthenticated = false`.
  - El navegador es inmediatamente redireccionado a `http://localhost:3000/login`.
- **Evidencia en Código**: `src/guards/ProtectedRoute.tsx`

---

### Escenario F2: Control de Acceso Basado en Roles (`RoleGuard`)
- **Acción**: Un usuario autenticado con el rol único `Client` intenta acceder a la ruta de reportería gerencial `http://localhost:3000/reports` o auditoría `http://localhost:3000/audit`.
- **Comportamiento Esperado**:
  - `RoleGuard` evalúa el arreglo `allowedRoles={['Admin']}` contra los roles del usuario (`['Client']`).
  - Al no encontrar coincidencia, cancela el renderizado y redirecciona a `/unauthorized`.
  - Se visualiza la pantalla **403 — Acceso Denegado** con el desglose de los roles actuales en el JWT.
- **Evidencia en Código**: `src/guards/RoleGuard.tsx` y `src/pages/UnauthorizedPage.tsx`.

---

## 2. Pruebas de Seguridad en Backend BFF (Spring Security OAuth2 Resource Server)

A continuación se detallan los comandos cURL y las respuestas HTTP esperadas para los 4 casos exigidos en la pauta de evaluación.

---

### Caso 1: Petición Sin Token Bearer
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/me
```
- **Resultado Esperado**: `HTTP/1.1 401 Unauthorized`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "timestamp": "2026-09-09T17:00:00.123456Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido, expirado o ausente",
  "path": "/api/me"
}
```
- **Componente Responsable**: `cl.duoc.vidasalud.bff.security.CustomAuthenticationEntryPoint`

---

### Caso 2: Petición con Token Expirado o Firma Inválida
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/appointments \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE2MDAwMDAwMDB9.invalid_signature"
```
- **Resultado Esperado**: `HTTP/1.1 401 Unauthorized`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "timestamp": "2026-09-09T17:01:15.890123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido, expirado o ausente",
  "path": "/api/appointments"
}
```
- **Componente Responsable**: `JwtDecoder` & `CustomAuthenticationEntryPoint`

---

### Caso 3: Token Válido pero Rol Incorrecto (Falta de Privilegios)
- **Contexto**: Petición enviada con un JWT válido emitido por Azure AD que contiene únicamente el rol `Client` intentando consultar `/api/report/kpis` (exclusivo para `Admin`).
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/report/kpis \
  -H "Authorization: Bearer <VALID_CLIENT_JWT_TOKEN>"
```
- **Resultado Esperado**: `HTTP/1.1 403 Forbidden`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "timestamp": "2026-09-09T17:02:30.456789Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Acceso denegado: rol o privilegio insuficiente para esta operación",
  "path": "/api/report/kpis"
}
```
- **Componente Responsable**: `CustomAccessDeniedHandler` & `SecurityConfig`

---

### Caso 4: Petición con Token Autenticado con Rol Admin
- **Contexto**: Petición enviada con un JWT válido emitido por Azure AD conteniendo el claim `"roles": ["Admin"]`.
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/me \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>"
```
- **Resultado Esperado**: `HTTP/1.1 200 OK`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "oid": "8f3b2a11-4c5d-6e7f-8a9b-0c1d2e3f4a5b",
  "name": "Administrador VidaSalud",
  "email": "admin@vidasalud.cl",
  "preferredUsername": "admin@vidasalud.cl",
  "roles": [
    "Admin"
  ],
  "scopes": [
    "access_as_user"
  ],
  "expiresAt": "2026-09-09T18:00:00Z",
  "issuedAt": "2026-09-09T17:00:00Z",
  "issuer": "https://login.microsoftonline.com/common/v2.0"
}
```
- **Componente Responsable**: `UserController` & `JwtAuthenticationConverterConfig`

---

## 3. Pruebas de Seguridad en Endpoints Administrativos de RabbitMQ (`/api/admin/amqp/**`)

Toda la jerarquía de rutas bajo `/api/admin/amqp/**` está explícitamente restringida al rol **Admin**.

---

### Caso 5: Petición Sin Token a API Administrativa RabbitMQ
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue
```
- **Resultado Esperado**: `HTTP/1.1 401 Unauthorized`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "timestamp": "2026-10-15T19:30:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido, expirado o ausente",
  "path": "/api/admin/amqp/queues/citas.agendamiento.queue"
}
```
- **Componente Responsable**: `CustomAuthenticationEntryPoint` & `SecurityConfig`

---

### Caso 6: Petición con Rol Distinto a Admin (ej. Paciente, Recepcionista, Auditor)
- **Contexto**: Petición enviada con JWT válido pero con claim `"roles": ["Recepcionista"]` o `"roles": ["Paciente"]`.
- **Petición**:
```bash
curl -i -X POST http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue/purge \
  -H "Authorization: Bearer <VALID_RECEPCIONISTA_OR_PACIENTE_JWT_TOKEN>"
```
- **Resultado Esperado**: `HTTP/1.1 403 Forbidden`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "timestamp": "2026-10-15T19:31:00.000Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Acceso denegado: rol o privilegio insuficiente para esta operación",
  "path": "/api/admin/amqp/queues/citas.agendamiento.queue/purge"
}
```
- **Componente Responsable**: `CustomAccessDeniedHandler`, `SecurityConfig` y `@PreAuthorize("hasRole('Admin')")`

---

### Caso 7: Petición con Rol Admin Autorizado
- **Contexto**: Petición enviada con JWT emitido por Microsoft Entra ID con claim `"roles": ["Admin"]`.
- **Petición**:
```bash
curl -i -X GET http://localhost:8080/api/admin/amqp/queues/citas.agendamiento.queue \
  -H "Authorization: Bearer <VALID_ADMIN_JWT_TOKEN>"
```
- **Resultado Esperado**: `HTTP/1.1 200 OK`
- **Cuerpo de la Respuesta JSON**:
```json
{
  "name": "citas.agendamiento.queue",
  "exists": true,
  "messageCount": 0,
  "consumerCount": 1,
  "apiSource": "Spring AMQP RabbitAdmin / queue.declare-passive",
  "details": "Estadísticas obtenidas vía protocolo AMQP 0-9-1. Métricas de memoria y tasas requieren Management HTTP Plugin."
}
```
- **Componente Responsable**: `RabbitAdminController`, `RabbitAdminService` y `SecurityConfig`

