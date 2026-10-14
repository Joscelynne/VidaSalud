# Arquitectura AWS VidaSalud - EP1 Cloud Native I

## Documento de Defensa Oral

**Fecha de generación:** 18 de septiembre de 2026  
**Proyecto:** VidaSalud - Sistema de Gestión de Citas Médicas  
**Asignatura:** DSY1107 Cloud Native I  
**Evaluación:** EP1  

---

# 1. INVENTARIO DE RECURSOS AWS

## 1.1 Tabla de Recursos

| Recurso | Identificador | Tipo | Región | Función |
|---------|---------------|------|--------|---------|
| **EC2** | `i-0dff7eb1e4daf94d0` | t2.micro | us-east-1a | Servidor que ejecuta contenedores Docker (Frontend + BFF) |
| **Elastic IP** | `eipalloc-0fc84119bde5c82ab` | IP Estática | us-east-1 | IP fija `3.235.125.162` para EC2, evita cambios de IP al reiniciar |
| **ECR (Frontend)** | `vidasalud-frontend` | Repositorio | us-east-1 | Almacena imagen Docker del frontend React |
| **ECR (BFF)** | `ms-vidasalud-bff` | Repositorio | us-east-1 | Almacena imagen Docker del backend Spring Boot |
| **API Gateway** | `nskclbdqa3` | HTTP API | us-east-1 | Punto de entrada HTTPS, valida JWT, enruta al BFF |
| **RDS PostgreSQL** | `vidasalud-db` | db.t3.micro | us-east-1a | Base de datos relacional para persistencia |
| **VPC** | `vpc-0b6f67f410b6fe53e` | Default VPC | us-east-1 | Red virtual que contiene todos los recursos |
| **Security Group EC2** | `sg-0edac46d3fb09b6df` | vidasalud-sg | us-east-1 | Firewall para EC2: puertos 22, 80, 8080 |
| **Security Group RDS** | `sg-0c192f8fae2d0eff0` | vidasalud-rds-sg | us-east-1 | Firewall para RDS: puerto 5432 solo desde EC2 |
| **DB Subnet Group** | `vidasalud-db-subnet-group` | Subnet Group | us-east-1 | Subnets us-east-1a y us-east-1b para RDS |

## 1.2 Detalle de Cada Recurso

### EC2 - vidasalud-ec2
```
Instance ID:      i-0dff7eb1e4daf94d0
Instance Type:    t2.micro (Free Tier eligible)
State:            running
Public IP:        3.235.125.162 (Elastic IP)
Private IP:       172.31.89.28
Subnet:           subnet-0e46f0de55cf4891d (us-east-1a)
VPC:              vpc-0b6f67f410b6fe53e
Security Group:   sg-0edac46d3fb09b6df (vidasalud-sg)
```

**Función:** Servidor virtual que ejecuta dos contenedores Docker:
- `vidasalud-frontend`: Nginx sirviendo la SPA React en puerto 80
- `vidasalud-bff`: Spring Boot ejecutando el BFF en puerto 8080

### Elastic IP
```
Public IP:        3.235.125.162
Allocation ID:    eipalloc-0fc84119bde5c82ab
Association ID:   eipassoc-0fd29973f32822a98
Attached to:      i-0dff7eb1e4daf94d0
```

**Función:** IP estática que no cambia cuando la EC2 se reinicia. Sin ella, cada reinicio asignaría una IP diferente y romperías todas las configuraciones de API Gateway, CORS y redirecciones de Azure AD.

### ECR Repositories
```
1. vidasalud-frontend
   URI: 756018908636.dkr.ecr.us-east-1.amazonaws.com/vidasalud-frontend
   
2. ms-vidasalud-bff
   URI: 756018908636.dkr.ecr.us-east-1.amazonaws.com/ms-vidasalud-bff
```

**Función:** Amazon Elastic Container Registry almacena las imágenes Docker versionadas. Desde aquí EC2 descarga las imágenes para ejecutar los contenedores.

### API Gateway - vidasalud-api
```
API ID:           nskclbdqa3
Name:             vidasalud-api
Type:             HTTP API
Endpoint:         https://nskclbdqa3.execute-api.us-east-1.amazonaws.com
Stage:            $default (auto-deploy enabled)
```

**Función:** Punto de entrada seguro con HTTPS. Valida tokens JWT de Azure AD antes de pasar las peticiones al BFF. Proporciona terminación SSL gratuita.

### RDS PostgreSQL - vidasalud-db
```
Identifier:       vidasalud-db
Engine:           PostgreSQL 15.17
Instance Class:   db.t3.micro (Free Tier eligible)
Status:           available
Endpoint:         vidasalud-db.c2xcy7q094zc.us-east-1.rds.amazonaws.com
Port:             5432
Database Name:    vidasalud
Publicly Access:  NO (solo accesible desde VPC)
Security Group:   sg-0c192f8fae2d0eff0
Subnet Group:     vidasalud-db-subnet-group
AZ:               us-east-1a
```

**Función:** Base de datos relacional que almacena las citas médicas (appointments). Solo es accesible desde la EC2 dentro de la misma VPC.

### Security Groups

**vidasalud-sg (sg-0edac46d3fb09b6df)**
| Puerto | Protocolo | Origen | Propósito |
|--------|-----------|--------|-----------|
| 22 | TCP | 0.0.0.0/0 | SSH para administración |
| 80 | TCP | 0.0.0.0/0 | Frontend Nginx (HTTP) |
| 8080 | TCP | 0.0.0.0/0 | BFF Spring Boot |

**vidasalud-rds-sg (sg-0c192f8fae2d0eff0)**
| Puerto | Protocolo | Origen | Propósito |
|--------|-----------|--------|-----------|
| 5432 | TCP | sg-0edac46d3fb09b6df | PostgreSQL solo desde EC2 |

**Conexión entre Security Groups:** El RDS SG permite conexiones en puerto 5432 SOLO desde recursos que tengan el SG de la EC2 (sg-0edac46d3fb09b6df). Esto significa que tu Mac no puede conectarse directamente al RDS, pero la EC2 sí puede.

---

# 2. ARQUITECTURA GENERAL - FLUJO COMPLETO

## 2.1 Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              FLUJO DE UNA PETICIÓN                               │
└─────────────────────────────────────────────────────────────────────────────────┘

     ┌──────────┐                                              
     │  USUARIO │  Abre navegador Safari/Chrome                
     │ (Safari) │                                              
     └────┬─────┘                                              
          │                                                    
          ▼                                                    
┌─────────────────────┐                                        
│   FRONTEND REACT    │  http://3.235.125.162                  
│   (Nginx en EC2)    │  Puerto 80                             
│                     │                                        
│  - SPA React 19     │                                        
│  - MSAL.js          │                                        
│  - TailwindCSS      │                                        
└─────────┬───────────┘                                        
          │                                                    
          │ ① Usuario no autenticado → Redirect               
          ▼                                                    
┌─────────────────────┐                                        
│  MICROSOFT ENTRA ID │  https://login.microsoftonline.com     
│    (Azure AD)       │                                        
│                     │                                        
│  - Autenticación    │                                        
│  - Genera JWT       │                                        
│  - Asigna roles     │                                        
└─────────┬───────────┘                                        
          │                                                    
          │ ② Retorna Access Token JWT con roles              
          ▼                                                    
┌─────────────────────┐                                        
│   FRONTEND REACT    │  Token guardado en sessionStorage      
│                     │                                        
│  axios interceptor  │  Añade: Authorization: Bearer <token>  
└─────────┬───────────┘                                        
          │                                                    
          │ ③ GET /api/appointments (con Bearer token)        
          ▼                                                    
┌─────────────────────┐                                        
│   AWS API GATEWAY   │  https://nskclbdqa3.execute-api...     
│   (HTTP API)        │                                        
│                     │                                        
│  ┌───────────────┐  │                                        
│  │ JWT AUTHORIZER│◄─┼── ④ Valida firma, issuer, audience     
│  │  (kwrr2w)     │  │     Si inválido → 401                  
│  └───────────────┘  │                                        
└─────────┬───────────┘                                        
          │                                                    
          │ ⑤ HTTP_PROXY → http://3.235.125.162:8080/api/...  
          ▼                                                    
┌─────────────────────┐                                        
│   SPRING BOOT BFF   │  Puerto 8080 en EC2                    
│   (Contenedor)      │                                        
│                     │                                        
│  ┌───────────────┐  │                                        
│  │SPRING SECURITY│◄─┼── ⑥ Re-valida JWT (issuer, audience,  
│  │ + JwtDecoder  │  │     expiration, signature)             
│  └───────┬───────┘  │                                        
│          │          │                                        
│  ┌───────▼───────┐  │                                        
│  │ JwtAuth       │◄─┼── ⑦ Extrae claim "roles" del JWT      
│  │ Converter     │  │     ["Admin"] → ROLE_Admin             
│  └───────┬───────┘  │                                        
│          │          │                                        
│  ┌───────▼───────┐  │                                        
│  │@PreAuthorize  │◄─┼── ⑧ Verifica hasRole('Admin')         
│  │ RBAC Check    │  │     Si no tiene rol → 403              
│  └───────┬───────┘  │                                        
│          │          │                                        
│  ┌───────▼───────┐  │                                        
│  │  Controller   │  │                                        
│  │    ↓          │  │                                        
│  │  Service      │  │                                        
│  │    ↓          │  │                                        
│  │  Repository   │◄─┼── ⑨ JPA ejecuta query SQL             
│  └───────┬───────┘  │                                        
└──────────┼──────────┘                                        
           │                                                   
           │ ⑩ SELECT * FROM appointments                     
           ▼                                                   
┌─────────────────────┐                                        
│  RDS POSTGRESQL     │  Puerto 5432 (privado)                 
│  vidasalud-db       │                                        
│                     │                                        
│  Tabla: appointments│                                        
└─────────┬───────────┘                                        
          │                                                    
          │ ⑪ Retorna filas                                   
          ▼                                                    
    (Respuesta sube por la misma cadena)                       
          │                                                    
          ▼                                                    
┌─────────────────────┐                                        
│   FRONTEND REACT    │  Renderiza datos en UI                 
└─────────────────────┘                                        
```

## 2.2 Explicación Paso a Paso

### Paso 1: Usuario accede al Frontend
- El usuario abre `http://3.235.125.162` en su navegador
- Nginx (corriendo en Docker en EC2, puerto 80) sirve los archivos estáticos de React
- React se carga en el navegador del usuario

### Paso 2: Autenticación con Microsoft Entra ID
- Si el usuario no tiene sesión, MSAL.js redirige a `login.microsoftonline.com`
- El usuario ingresa credenciales de Azure AD
- Microsoft Entra ID verifica credenciales y genera un **Access Token JWT**
- El JWT contiene:
  - `iss` (issuer): quién emitió el token
  - `aud` (audience): para quién es el token
  - `exp` (expiration): cuándo expira
  - `roles`: array con roles del usuario (ej: `["Paciente"]`)
  - `preferred_username`: email del usuario
  - Firma criptográfica (RS256)

### Paso 3: Frontend envía petición con token
- El token se guarda en `sessionStorage`
- Axios interceptor automáticamente añade el header:
  ```
  Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIm...
  ```
- La petición va a API Gateway (no directamente al BFF)

### Paso 4: API Gateway valida el JWT (primera validación)
- El **JWT Authorizer** (`kwrr2w`) verifica:
  - ✅ Firma válida (usando JWKS de Microsoft)
  - ✅ Issuer = `https://login.microsoftonline.com/877a72d5-.../v2.0`
  - ✅ Audience = `693636ac-8558-4324-851a-974c8dce1005`
  - ✅ Token no expirado
- Si falla cualquier validación → **HTTP 401 Unauthorized**

### Paso 5: API Gateway reenvía al BFF
- Si el JWT es válido, API Gateway hace proxy HTTP a:
  ```
  http://3.235.125.162:8080/api/{proxy}
  ```
- El header Authorization se pasa intacto al BFF

### Paso 6-7: Spring Security re-valida el JWT (segunda validación)
- **JwtDecoder** verifica nuevamente firma, issuer, audience, expiration
- **JwtAuthenticationConverter** extrae el claim `roles` y lo convierte:
  - JWT claim: `"roles": ["Paciente"]`
  - Spring Authority: `ROLE_Paciente`

### Paso 8: Verificación de roles (RBAC)
- `SecurityConfig` define qué roles pueden acceder a qué endpoints
- `@PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")` verifica
- Si el usuario no tiene el rol requerido → **HTTP 403 Forbidden**

### Paso 9-10: Acceso a base de datos
- El Controller llama al Service
- El Service llama al Repository
- JPA traduce el método a SQL:
  ```sql
  SELECT * FROM appointments
  ```
- El query viaja por la VPC al RDS PostgreSQL

### Paso 11: Respuesta
- PostgreSQL retorna las filas
- El Service convierte Entity → DTO
- El Controller retorna JSON
- La respuesta sube por API Gateway hasta el navegador
- React renderiza los datos

---

# 3. API GATEWAY - CONFIGURACIÓN DETALLADA

## 3.1 Información General
```
API ID:       nskclbdqa3
Name:         vidasalud-api
Type:         HTTP API (no REST API)
Endpoint:     https://nskclbdqa3.execute-api.us-east-1.amazonaws.com
Protocol:     HTTPS (terminación SSL gratuita)
Stage:        $default (auto-deploy habilitado)
```

## 3.2 Rutas Configuradas

| Route Key | Método | Autorización | Integración | Propósito |
|-----------|--------|--------------|-------------|-----------|
| `ANY /api/{proxy+}` | ANY | JWT (kwrr2w) | `http://3.235.125.162:8080/api/{proxy}` | Todas las llamadas API |
| `OPTIONS /api/{proxy+}` | OPTIONS | NONE | `http://3.235.125.162:8080/api/{proxy}` | CORS preflight |
| `GET /actuator/health` | GET | NONE | `http://3.235.125.162:8080/actuator/health` | Health check público |

### Explicación de `ANY /api/{proxy+}`

- `ANY`: Acepta cualquier método HTTP (GET, POST, PUT, DELETE, PATCH)
- `/api/{proxy+}`: Captura todo después de `/api/`
  - `{proxy+}` es un "greedy path parameter"
  - `/api/appointments` → `{proxy}` = `appointments`
  - `/api/appointments/APT-101/confirm` → `{proxy}` = `appointments/APT-101/confirm`

### ¿Por qué la integración usa `/api/{proxy}` y no `/{proxy}`?

La **Integration URI** es: `http://3.235.125.162:8080/api/{proxy}`

Si usáramos `/{proxy}` (sin `/api`):
- Request: `GET /api/appointments`
- Se extraería: `{proxy}` = `appointments`
- Se enviaría a: `http://3.235.125.162:8080/appointments` ❌
- Spring recibiría `/appointments` pero el endpoint es `/api/appointments`
- Resultado: **404 Not Found**

Con `/api/{proxy}`:
- Request: `GET /api/appointments`
- Se extraería: `{proxy}` = `appointments`
- Se enviaría a: `http://3.235.125.162:8080/api/appointments` ✅
- Spring recibe `/api/appointments` correctamente
- Resultado: **200 OK**

## 3.3 JWT Authorizer

```json
{
  "AuthorizerId": "kwrr2w",
  "AuthorizerType": "JWT",
  "Name": "vidasalud-jwt-authorizer",
  "IdentitySource": ["$request.header.Authorization"],
  "JwtConfiguration": {
    "Issuer": "https://login.microsoftonline.com/877a72d5-ba5e-45e0-8b5c-ebb0268f993a/v2.0",
    "Audience": ["693636ac-8558-4324-851a-974c8dce1005"]
  }
}
```

**Identity Source:** `$request.header.Authorization`
- Busca el token en el header `Authorization`
- Espera formato: `Bearer <token>`

**Issuer:** `https://login.microsoftonline.com/{tenant-id}/v2.0`
- Verifica que el token fue emitido por TU tenant de Azure AD
- Si alguien intenta usar un token de otro tenant → 401

**Audience:** `693636ac-8558-4324-851a-974c8dce1005`
- Verifica que el token fue emitido para TU aplicación
- Coincide con el Client ID del App Registration en Azure

## 3.4 Configuración CORS

```json
{
  "AllowCredentials": false,
  "AllowHeaders": ["authorization", "content-type", "x-requested-with"],
  "AllowMethods": ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
  "AllowOrigins": [
    "http://localhost:3000",
    "http://localhost:5173", 
    "http://3.235.125.162",
    "https://nskclbdqa3.execute-api.us-east-1.amazonaws.com"
  ],
  "ExposeHeaders": ["*"],
  "MaxAge": 86400
}
```

**AllowOrigins:** Orígenes permitidos para hacer peticiones
- `localhost:3000` y `localhost:5173`: Desarrollo local
- `3.235.125.162`: Frontend en producción
- URL de API Gateway: Para casos donde el frontend se sirva desde ahí

**AllowHeaders:** `authorization` es crítico para enviar el Bearer token

**AllowMethods:** Todos los métodos HTTP necesarios para la API REST

---

# 4. SEGURIDAD - CADENA COMPLETA

## 4.1 Flujo de Seguridad

```
┌────────────────────────────────────────────────────────────────┐
│                    CADENA DE SEGURIDAD                          │
└────────────────────────────────────────────────────────────────┘

    MICROSOFT ENTRA ID (Azure AD)
           │
           │ Genera JWT firmado con RS256
           │ Incluye: iss, aud, exp, roles, preferred_username
           ▼
    ┌──────────────────┐
    │   ACCESS TOKEN   │  eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1...
    │      (JWT)       │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │  API GATEWAY     │  VALIDACIÓN #1
    │  JWT AUTHORIZER  │
    │                  │  ✓ Firma válida (JWKS)
    │                  │  ✓ Issuer correcto
    │                  │  ✓ Audience correcta
    │                  │  ✓ No expirado
    │                  │
    │  Si falla → 401  │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │  SPRING SECURITY │  VALIDACIÓN #2
    │  JwtDecoder      │
    │                  │  ✓ Firma válida (JWKS)
    │                  │  ✓ Issuer correcto (v1 o v2)
    │                  │  ✓ Audience correcta
    │                  │  ✓ Timestamps válidos
    │                  │
    │  Si falla → 401  │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │  JwtAuth         │  EXTRACCIÓN DE ROLES
    │  Converter       │
    │                  │  JWT claim "roles": ["Paciente"]
    │                  │         ↓
    │                  │  Spring: ROLE_Paciente
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │  SecurityConfig  │  AUTORIZACIÓN (RBAC)
    │  + @PreAuthorize │
    │                  │  ¿Usuario tiene rol requerido?
    │                  │  
    │  Si no → 403     │
    └────────┬─────────┘
             │
             ▼
    ┌──────────────────┐
    │  CONTROLLER      │  Ejecuta lógica de negocio
    └──────────────────┘
```

## 4.2 Claims del JWT Explicados

| Claim | Nombre | Ejemplo | Dónde se valida |
|-------|--------|---------|-----------------|
| `iss` | Issuer | `https://login.microsoftonline.com/{tenant}/v2.0` | API Gateway + Spring |
| `aud` | Audience | `693636ac-8558-4324-851a-974c8dce1005` | API Gateway + Spring |
| `exp` | Expiration | `1726704000` (Unix timestamp) | API Gateway + Spring |
| `iat` | Issued At | `1726700400` | Spring (timestamp validator) |
| `nbf` | Not Before | `1726700400` | Spring (timestamp validator) |
| `sub` | Subject | `a1b2c3d4-...` (User Object ID) | No validado, solo info |
| `roles` | Roles | `["Admin", "Paciente"]` | Spring (JwtAuthConverter) |
| `preferred_username` | Email | `admin@vidasalud.cl` | No validado, solo info |
| `scp` | Scopes | `access_as_user` | No validado en esta app |

### Issuer
- **Qué es:** Identifica QUIÉN emitió el token
- **Valor esperado:** URL del tenant de Azure AD
- **Por qué importa:** Garantiza que el token viene de TU Azure AD, no de otro

### Audience
- **Qué es:** Identifica PARA QUIÉN es el token
- **Valor esperado:** Client ID de tu App Registration
- **Por qué importa:** Un token emitido para otra aplicación no puede usarse aquí

### Signature (Firma)
- **Qué es:** Hash criptográfico RS256 de header + payload
- **Cómo se valida:** Se obtienen las claves públicas del JWKS endpoint de Microsoft
- **Por qué importa:** Garantiza que el token no fue modificado

### Expiration
- **Qué es:** Timestamp Unix cuando el token deja de ser válido
- **Duración típica:** 1 hora (configurable en Azure AD)
- **Por qué importa:** Tokens robados tienen ventana limitada de uso

## 4.3 ¿Por qué doble validación de JWT?

**Validación 1 (API Gateway):** 
- Protege la infraestructura AWS
- Rechaza peticiones inválidas ANTES de llegar a tu servidor
- Reduce carga en EC2

**Validación 2 (Spring Security):**
- Defensa en profundidad
- Extrae información del token (roles, username)
- Permite lógica de autorización más compleja
- Funciona incluso si alguien bypasea API Gateway

## 4.4 Conversión de Roles: JWT → Spring

```java
// JwtAuthenticationConverterConfig.java

// 1. Extraer claim 'roles' del JWT
List<String> roles = jwt.getClaimAsStringList("roles");
// roles = ["Admin", "Paciente"]

// 2. Convertir cada rol a GrantedAuthority con prefijo ROLE_
for (String role : roles) {
    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
}
// authorities = [ROLE_Admin, ROLE_Paciente]
```

**¿Por qué el prefijo `ROLE_`?**
- Convención de Spring Security
- Métodos como `hasRole('Admin')` automáticamente buscan `ROLE_Admin`
- Permite distinguir roles de otros tipos de authorities (como scopes)

---

# 5. RBAC - MATRIZ DE PERMISOS

## 5.1 Roles del Sistema

| Rol | Descripción | Usuarios típicos |
|-----|-------------|------------------|
| **Admin** | Acceso completo al sistema | Administradores de TI |
| **Recepcionista** | Gestión de citas y catálogo | Personal de recepción |
| **Paciente** | Consulta de sus propias citas | Pacientes registrados |
| **Auditor** | Acceso a logs de auditoría | Personal de cumplimiento |

## 5.2 Matriz de Permisos por Endpoint

| Endpoint | Método | Admin | Recepcionista | Paciente | Auditor |
|----------|--------|-------|---------------|----------|---------|
| `/api/appointments` | GET | ✅ | ✅ | ✅ | ❌ |
| `/api/appointments/{id}` | GET | ✅ | ✅ | ✅ | ❌ |
| `/api/appointments/{id}/confirm` | POST | ✅ | ✅ | ❌ | ❌ |
| `/api/catalog/specialties` | GET | ✅ | ✅ | ❌ | ❌ |
| `/api/report/kpis` | GET | ✅ | ❌ | ❌ | ❌ |
| `/api/audit/logs` | GET | ✅ | ❌ | ❌ | ✅ |
| `/api/me` | GET | ✅ | ✅ | ✅ | ✅ |
| `/actuator/health` | GET | 🌐 | 🌐 | 🌐 | 🌐 |

**Leyenda:** ✅ Permitido | ❌ Denegado | 🌐 Público (sin autenticación)

## 5.3 Configuración en Código

```java
// SecurityConfig.java

.authorizeHttpRequests(auth -> auth
    // Público (sin autenticación)
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

    // Atenciones Médicas: Admin, Recepcionista, Paciente
    .requestMatchers("/api/appointments/**")
        .hasAnyRole("Admin", "Recepcionista", "Paciente")

    // Catálogo: Admin, Recepcionista
    .requestMatchers("/api/catalog/**")
        .hasAnyRole("Admin", "Recepcionista")

    // Reportería: Solo Admin
    .requestMatchers("/api/report/**")
        .hasRole("Admin")

    // Auditoría: Admin, Auditor
    .requestMatchers("/api/audit/**")
        .hasAnyRole("Admin", "Auditor")

    // /api/me: Cualquier usuario autenticado
    .requestMatchers("/api/me").authenticated()

    // Todo lo demás requiere autenticación
    .anyRequest().authenticated()
)
```

```java
// AppointmentController.java (validación adicional a nivel de método)

@PostMapping("/{id}/confirm")
@PreAuthorize("hasAnyRole('Admin', 'Recepcionista')")  // Solo Admin y Recepcionista
public ResponseEntity<AppointmentDto> confirmAppointment(@PathVariable String id) {
    // ...
}
```

## 5.4 Respuestas HTTP según Escenario

| Escenario | Código HTTP | Mensaje | Manejador |
|-----------|-------------|---------|-----------|
| Sin token | **401** | "Token inválido, expirado o ausente" | API Gateway o CustomAuthenticationEntryPoint |
| Token expirado | **401** | "Token inválido, expirado o ausente" | API Gateway o CustomAuthenticationEntryPoint |
| Token de otro tenant | **401** | "Token inválido, expirado o ausente" | API Gateway |
| Token válido, rol incorrecto | **403** | "Acceso denegado: rol o privilegio insuficiente" | CustomAccessDeniedHandler |
| Token válido, rol correcto | **200** | Datos solicitados | Controller |

---

# 6. EC2 - CONTENIDO Y CONFIGURACIÓN

## 6.1 Información del Servidor

```
Instance ID:      i-0dff7eb1e4daf94d0
Instance Type:    t2.micro
AMI:              Amazon Linux 2023
Public IP:        3.235.125.162 (Elastic IP)
Private IP:       172.31.89.28
Availability Zone: us-east-1a
```

## 6.2 Contenedores Docker en Ejecución

| Contenedor | Imagen | Puerto Host | Puerto Container | Proceso |
|------------|--------|-------------|------------------|---------|
| `vidasalud-frontend` | vidasalud-frontend:latest | 80 | 80 | Nginx |
| `vidasalud-bff` | ms-vidasalud-bff:latest | 8080 | 8080 | Java 21 |

## 6.3 Acceso a los Servicios

**Frontend (Nginx):**
```
URL Directa:    http://3.235.125.162/
Puerto:         80
Servidor:       Nginx Alpine
Contenido:      Archivos estáticos de React (dist/)
Configuración:  SPA fallback a index.html
```

**BFF (Spring Boot):**
```
URL Directa:    http://3.235.125.162:8080/
Puerto:         8080
Servidor:       Embedded Tomcat (Spring Boot)
Health Check:   http://3.235.125.162:8080/actuator/health
```

## 6.4 Comunicación BFF → RDS

```
BFF Container (172.31.89.28:8080)
         │
         │ JDBC Connection
         │ jdbc:postgresql://vidasalud-db.c2xcy7q094zc.us-east-1.rds.amazonaws.com:5432/vidasalud
         │
         ▼
RDS PostgreSQL (Private, solo accesible desde VPC)
         │
         │ Security Group permite puerto 5432
         │ SOLO desde sg-0edac46d3fb09b6df (EC2 SG)
         ▼
```

La conexión funciona porque:
1. EC2 y RDS están en la misma VPC (`vpc-0b6f67f410b6fe53e`)
2. El Security Group del RDS permite conexiones desde el SG de la EC2
3. El driver PostgreSQL resuelve el endpoint DNS del RDS dentro de la VPC

## 6.5 Rol de Nginx

Nginx actúa como:
1. **Servidor de archivos estáticos:** Sirve el bundle de React (index.html, JS, CSS)
2. **SPA Router:** Redirige todas las rutas a index.html para React Router
3. **Proxy reverso opcional:** Puede redirigir `/api/*` al BFF (configurado pero no usado en producción)

```nginx
# nginx.conf

# SPA Fallback
location / {
    try_files $uri $uri/ /index.html;
}

# Proxy a BFF (usado en desarrollo local con Docker Compose)
location /api/ {
    proxy_pass http://bff:8080/api/;
}
```

## 6.6 Proceso de Despliegue ECR → EC2

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   CÓDIGO    │     │    ECR      │     │    EC2      │
│   LOCAL     │────▶│  REGISTRY   │────▶│  CONTAINER  │
└─────────────┘     └─────────────┘     └─────────────┘

1. docker build -t vidasalud-frontend .
2. docker tag vidasalud-frontend:latest 756018908636.dkr.ecr.us-east-1.amazonaws.com/vidasalud-frontend:latest
3. aws ecr get-login-password | docker login --username AWS --password-stdin 756018908636.dkr.ecr...
4. docker push 756018908636.dkr.ecr.us-east-1.amazonaws.com/vidasalud-frontend:latest

(En EC2):
5. aws ecr get-login-password | docker login ...
6. docker pull 756018908636.dkr.ecr.us-east-1.amazonaws.com/vidasalud-frontend:latest
7. docker run -d -p 80:80 --name vidasalud-frontend vidasalud-frontend:latest
```

---

# 7. ECR + DOCKER

## 7.1 Repositorios ECR

```
Cuenta AWS:  756018908636
Región:      us-east-1

Repositorio 1: vidasalud-frontend
URI: 756018908636.dkr.ecr.us-east-1.amazonaws.com/vidasalud-frontend

Repositorio 2: ms-vidasalud-bff
URI: 756018908636.dkr.ecr.us-east-1.amazonaws.com/ms-vidasalud-bff
```

## 7.2 Imagen Frontend

**Dockerfile (Multi-stage build):**
```dockerfile
# Etapa 1: Build con Node.js
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build  # Genera dist/

# Etapa 2: Servidor Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**Contenido de la imagen:**
- Nginx Alpine (servidor web ligero)
- Archivos estáticos: `index.html`, `assets/index-*.js`, `assets/index-*.css`
- Configuración Nginx personalizada

**Tamaño aproximado:** ~25MB

## 7.3 Imagen BFF

**Dockerfile (Multi-stage build):**
```dockerfile
# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests  # Genera target/ms-vidasalud-bff-1.0.0-SNAPSHOT.jar

# Etapa 2: Runtime con JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ms-vidasalud-bff-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Contenido de la imagen:**
- Eclipse Temurin JRE 21 (runtime Java)
- JAR ejecutable de Spring Boot (incluye Tomcat embebido)
- Variables de entorno para Azure AD y DB

**Tamaño aproximado:** ~300MB

## 7.4 Flujo Completo de Despliegue

```
┌─────────────────────────────────────────────────────────────┐
│                    PIPELINE DE DESPLIEGUE                    │
└─────────────────────────────────────────────────────────────┘

   Código Fuente          Docker Build            ECR Push
   ┌─────────┐           ┌─────────┐           ┌─────────┐
   │  .java  │──────────▶│  Image  │──────────▶│  ECR    │
   │  .tsx   │  Dockerfile│  local  │  docker   │ Registry│
   │  .ts    │           │         │  push     │         │
   └─────────┘           └─────────┘           └─────────┘
                                                    │
                                                    │ docker pull
                                                    ▼
                              EC2 Run            ┌─────────┐
                             ┌─────────┐         │   EC2   │
                             │Container│◀────────│ Instance│
                             │ Running │         │         │
                             └─────────┘         └─────────┘
```

---

# 8. RDS POSTGRESQL

## 8.1 Configuración del Servidor

```
Identificador:        vidasalud-db
Motor:                PostgreSQL
Versión:              15.17
Clase de instancia:   db.t3.micro (2 vCPU, 1GB RAM)
Almacenamiento:       20 GB SSD (gp2)
Estado:               available
```

## 8.2 Conectividad

```
Endpoint:             vidasalud-db.c2xcy7q094zc.us-east-1.rds.amazonaws.com
Puerto:               5432
Nombre de BD:         vidasalud
Usuario:              vidasalud_admin
Acceso Público:       NO
VPC:                  vpc-0b6f67f410b6fe53e
Subnet Group:         vidasalud-db-subnet-group
Security Group:       sg-0c192f8fae2d0eff0 (vidasalud-rds-sg)
Availability Zone:    us-east-1a
```

## 8.3 Subnets del DB Subnet Group

| Subnet ID | CIDR | Availability Zone |
|-----------|------|-------------------|
| subnet-0e46f0de55cf4891d | 172.31.80.0/20 | us-east-1a |
| subnet-028d03b6aeeaad081 | 172.31.16.0/20 | us-east-1b |

**¿Por qué dos subnets?**
- Requisito de RDS para alta disponibilidad
- Si la AZ principal falla, RDS puede failover a la secundaria
- Aunque estamos en Single-AZ, el subnet group debe tener al menos 2 AZs

## 8.4 ¿Por qué Mac → RDS NO funciona pero EC2 → RDS SÍ?

```
┌─────────────────────────────────────────────────────────────┐
│                      DIAGRAMA DE RED                         │
└─────────────────────────────────────────────────────────────┘

    ┌──────────┐                          ┌──────────────────┐
    │   TU     │                          │      AWS VPC     │
    │   MAC    │                          │                  │
    │          │                          │  ┌────────────┐  │
    │ (Internet)─────────────X────────────│──│    RDS     │  │
    │          │     No hay ruta          │  │  (Private) │  │
    └──────────┘                          │  └────────────┘  │
                                          │        ▲         │
                                          │        │ ✓       │
                                          │        │         │
                                          │  ┌────────────┐  │
                                          │  │    EC2     │  │
                                          │  │  (Public)  │  │
                                          │  └────────────┘  │
                                          │                  │
                                          └──────────────────┘
```

**Razones técnicas:**

1. **RDS NO tiene IP pública:** `PubliclyAccessible: false`
   - No existe una IP pública para conectarse desde Internet
   
2. **Security Group restrictivo:**
   - Solo permite conexiones desde `sg-0edac46d3fb09b6df` (el SG de la EC2)
   - Tu Mac no pertenece a ese Security Group
   
3. **Sin NAT Gateway / Bastion:**
   - No hay forma de "entrar" a la VPC desde Internet para alcanzar el RDS

**¿Por qué EC2 sí puede?**
- EC2 está DENTRO de la VPC (IP privada 172.31.89.28)
- EC2 tiene asignado el Security Group sg-0edac46d3fb09b6df
- El SG del RDS permite conexiones desde ese SG específico
- La comunicación es directa por la red privada de AWS

## 8.5 Tabla appointments

```sql
CREATE TABLE appointments (
    id VARCHAR(50) PRIMARY KEY,
    patient_name VARCHAR(200) NOT NULL,
    doctor_name VARCHAR(200) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    date_time VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    room_number VARCHAR(50),
    notes VARCHAR(500)
);
```

**Datos actuales:**
| id | patient_name | doctor_name | specialty | status |
|----|--------------|-------------|-----------|--------|
| APT-101 | Carlos Mendoza | Dra. Patricia Vega | Medicina General | PENDING |
| APT-102 | Lorena Silva | Dr. Andrés Muñoz | Pediatría | CONFIRMED |
| APT-103 | Felipe Araya | Dra. Camila Reyes | Cardiología | COMPLETED |

---

# 9. PERSISTENCIA - FLUJO COMPLETO

## 9.1 Capas de la Aplicación

```
┌─────────────────────────────────────────────────────────────┐
│                     CAPAS DEL BFF                            │
└─────────────────────────────────────────────────────────────┘

    HTTP Request: GET /api/appointments
                    │
                    ▼
    ┌───────────────────────────────┐
    │        CONTROLLER             │  AppointmentController.java
    │  @RestController              │
    │  @RequestMapping("/api/...")  │
    │                               │
    │  Recibe HTTP, delega a Service│
    └───────────────┬───────────────┘
                    │
                    ▼
    ┌───────────────────────────────┐
    │          SERVICE              │  AppointmentService.java
    │  @Service                     │
    │  @Transactional               │
    │                               │
    │  Lógica de negocio            │
    │  Conversión Entity ↔ DTO      │
    └───────────────┬───────────────┘
                    │
                    ▼
    ┌───────────────────────────────┐
    │        REPOSITORY             │  AppointmentRepository.java
    │  @Repository                  │
    │  extends JpaRepository        │
    │                               │
    │  Métodos CRUD automáticos     │
    │  + queries personalizados     │
    └───────────────┬───────────────┘
                    │
                    ▼
    ┌───────────────────────────────┐
    │          ENTITY               │  Appointment.java
    │  @Entity                      │
    │  @Table(name="appointments")  │
    │                               │
    │  Mapeo Java ↔ SQL             │
    └───────────────┬───────────────┘
                    │
                    │ JDBC (HikariCP Connection Pool)
                    ▼
    ┌───────────────────────────────┐
    │       POSTGRESQL RDS          │  vidasalud-db
    │                               │
    │  Tabla: appointments          │
    └───────────────────────────────┘
```

## 9.2 Código de Cada Capa

### Controller
```java
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
        List<AppointmentDto> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }
}
```

### Service
```java
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<AppointmentDto> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        return appointments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AppointmentDto toDto(Appointment entity) {
        return AppointmentDto.builder()
                .id(entity.getId())
                .patientName(entity.getPatientName())
                // ... otros campos
                .build();
    }
}
```

### Repository
```java
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    // JpaRepository provee automáticamente:
    // - findAll()
    // - findById(id)
    // - save(entity)
    // - deleteById(id)
    // - count()
    // etc.

    // Métodos custom (Spring Data genera la query automáticamente):
    List<Appointment> findByStatus(String status);
}
```

### Entity
```java
@Entity
@Table(name = "appointments")
@Data
@Builder
public class Appointment {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "specialty", nullable = false)
    private String specialty;

    @Column(name = "date_time", nullable = false)
    private String dateTime;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "notes")
    private String notes;
}
```

## 9.3 Query SQL Generado por JPA

Cuando se ejecuta `appointmentRepository.findAll()`, Hibernate genera:

```sql
SELECT 
    a.id,
    a.patient_name,
    a.doctor_name,
    a.specialty,
    a.date_time,
    a.status,
    a.room_number,
    a.notes
FROM 
    appointments a
```

## 9.4 ¿Cómo sabemos que la persistencia es REAL?

1. **ddl-auto: validate**
   - Spring NO crea ni modifica tablas
   - La tabla debe existir previamente en RDS
   - Si la tabla no existe o no coincide → error al iniciar
   
2. **Health Check muestra PostgreSQL UP**
   ```json
   {
     "status": "UP",
     "components": {
       "db": {
         "status": "UP",
         "details": {
           "database": "PostgreSQL",
           "validationQuery": "isValid()"
         }
       }
     }
   }
   ```

3. **Los cambios persisten entre reinicios**
   - Si confirmas APT-101 → status cambia a CONFIRMED
   - Reinicias el contenedor BFF
   - APT-101 sigue siendo CONFIRMED (dato en RDS, no en memoria)

---

# 10. RED Y SEGURIDAD

## 10.1 Diagrama de Red

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              INTERNET                                    │
│                                                                          │
│     Usuario                                                              │
│        │                                                                 │
│        ▼                                                                 │
│  ┌───────────────────┐                                                   │
│  │   API GATEWAY     │  HTTPS (443)                                      │
│  │   (AWS Managed)   │  Endpoint público                                 │
│  └─────────┬─────────┘                                                   │
│            │                                                             │
└────────────┼─────────────────────────────────────────────────────────────┘
             │
             │ HTTP (8080)
             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         AWS VPC (172.31.0.0/16)                          │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │              SUBNET us-east-1a (172.31.80.0/20)                  │    │
│  │                                                                  │    │
│  │   ┌─────────────────┐         ┌─────────────────┐               │    │
│  │   │      EC2        │         │       RDS       │               │    │
│  │   │  172.31.89.28   │────────▶│   (Private)     │               │    │
│  │   │                 │  5432   │                 │               │    │
│  │   │  SG: vidasalud- │         │  SG: vidasalud- │               │    │
│  │   │      sg         │         │      rds-sg     │               │    │
│  │   │                 │         │                 │               │    │
│  │   │ ┌─────────────┐ │         │  vidasalud-db   │               │    │
│  │   │ │  Frontend   │ │         │  PostgreSQL     │               │    │
│  │   │ │  :80        │ │         │  15.17          │               │    │
│  │   │ ├─────────────┤ │         │                 │               │    │
│  │   │ │  BFF        │ │         │  NO Public IP   │               │    │
│  │   │ │  :8080      │ │         │                 │               │    │
│  │   │ └─────────────┘ │         └─────────────────┘               │    │
│  │   │                 │                                            │    │
│  │   │  Public IP:     │                                            │    │
│  │   │  3.235.125.162  │                                            │    │
│  │   │  (Elastic IP)   │                                            │    │
│  │   └─────────────────┘                                            │    │
│  │                                                                  │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 10.2 Security Groups - Reglas de Firewall

### vidasalud-sg (sg-0edac46d3fb09b6df) - Para EC2

**Inbound (Entrada):**
| Puerto | Protocolo | Origen | Uso |
|--------|-----------|--------|-----|
| 22 | TCP | 0.0.0.0/0 | SSH desde cualquier IP |
| 80 | TCP | 0.0.0.0/0 | Frontend Nginx |
| 8080 | TCP | 0.0.0.0/0 | BFF Spring Boot |

**Outbound (Salida):**
| Puerto | Protocolo | Destino | Uso |
|--------|-----------|---------|-----|
| Todo | Todo | 0.0.0.0/0 | Permite todas las conexiones salientes |

### vidasalud-rds-sg (sg-0c192f8fae2d0eff0) - Para RDS

**Inbound (Entrada):**
| Puerto | Protocolo | Origen | Uso |
|--------|-----------|--------|-----|
| 5432 | TCP | sg-0edac46d3fb09b6df | PostgreSQL SOLO desde EC2 |

**Outbound (Salida):**
| Puerto | Protocolo | Destino | Uso |
|--------|-----------|---------|-----|
| Todo | Todo | 0.0.0.0/0 | Conexiones salientes |

## 10.3 Acceso Público vs Privado

| Recurso | Acceso Público | Acceso desde VPC | Motivo |
|---------|----------------|------------------|--------|
| API Gateway | ✅ Sí (HTTPS) | N/A | Punto de entrada para usuarios |
| EC2 (Frontend) | ✅ Sí (HTTP:80) | ✅ Sí | Sirve archivos estáticos a usuarios |
| EC2 (BFF) | ✅ Sí (HTTP:8080) | ✅ Sí | API Gateway conecta aquí |
| RDS | ❌ No | ✅ Sí | Solo el BFF necesita acceso |

**¿Por qué RDS es privado?**
- **Seguridad:** Reduce superficie de ataque
- **Compliance:** Datos sensibles (pacientes) no expuestos a Internet
- **Best Practice:** Bases de datos nunca deben ser públicas en producción

---

# 11. DIAGRAMA ASCII DE ARQUITECTURA COMPLETA

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           ARQUITECTURA VIDASALUD - EP1                                   │
└─────────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────────┐
                                    │     USUARIO     │
                                    │    (Browser)    │
                                    └────────┬────────┘
                                             │
                                             │ HTTPS
                                             ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                   MICROSOFT ENTRA ID                                    │
│                               (Azure Active Directory)                                  │
│                                                                                         │
│   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐                    │
│   │  Autenticación  │    │   App Registry  │    │  Roles (Claims) │                    │
│   │  OAuth 2.0      │    │   Client ID:    │    │  - Admin        │                    │
│   │  OIDC           │    │   693636ac-...  │    │  - Recepcionista│                    │
│   │                 │    │                 │    │  - Paciente     │                    │
│   │  Genera JWT     │    │  Tenant ID:     │    │  - Auditor      │                    │
│   │  Access Token   │    │  877a72d5-...   │    │                 │                    │
│   └─────────────────┘    └─────────────────┘    └─────────────────┘                    │
└────────────────────────────────────────────────────────────────────────────────────────┘
                                             │
                                             │ JWT Access Token
                                             ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                      AWS CLOUD                                          │
│                                                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                              API GATEWAY (HTTP API)                               │  │
│  │                         nskclbdqa3.execute-api.us-east-1                          │  │
│  │                                                                                   │  │
│  │   ┌─────────────────────────────────────────────────────────────────────────┐    │  │
│  │   │                        JWT AUTHORIZER (kwrr2w)                           │    │  │
│  │   │                                                                          │    │  │
│  │   │   Issuer:   https://login.microsoftonline.com/877a72d5-.../v2.0         │    │  │
│  │   │   Audience: 693636ac-8558-4324-851a-974c8dce1005                         │    │  │
│  │   │                                                                          │    │  │
│  │   │   ✓ Valida firma      ✓ Valida issuer     ✓ Valida audience             │    │  │
│  │   │   ✓ Valida expiration                                                    │    │  │
│  │   │                                                                          │    │  │
│  │   │   Si inválido → HTTP 401 Unauthorized                                    │    │  │
│  │   └─────────────────────────────────────────────────────────────────────────┘    │  │
│  │                                                                                   │  │
│  │   Routes:                                                                         │  │
│  │   ├── ANY /api/{proxy+}      → JWT Required → http://3.235.125.162:8080/api/...  │  │
│  │   ├── OPTIONS /api/{proxy+}  → No Auth      → CORS Preflight                     │  │
│  │   └── GET /actuator/health   → No Auth      → Health Check                       │  │
│  │                                                                                   │  │
│  └────────────────────────────────────────┬─────────────────────────────────────────┘  │
│                                           │                                            │
│                                           │ HTTP (8080)                                │
│                                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                           VPC (vpc-0b6f67f410b6fe53e)                             │  │
│  │                                172.31.0.0/16                                      │  │
│  │                                                                                   │  │
│  │   ┌─────────────────────────────────────────────────────────────────────────┐    │  │
│  │   │                    EC2 (i-0dff7eb1e4daf94d0)                             │    │  │
│  │   │                    t2.micro | us-east-1a                                 │    │  │
│  │   │                    Elastic IP: 3.235.125.162                             │    │  │
│  │   │                    SG: vidasalud-sg                                      │    │  │
│  │   │                                                                          │    │  │
│  │   │   ┌────────────────────────────────────────────────────────────────┐    │    │  │
│  │   │   │                    DOCKER CONTAINERS                           │    │    │  │
│  │   │   │                                                                │    │    │  │
│  │   │   │   ┌────────────────────┐    ┌────────────────────────────┐    │    │    │  │
│  │   │   │   │ vidasalud-frontend │    │      vidasalud-bff         │    │    │    │  │
│  │   │   │   │                    │    │                            │    │    │    │  │
│  │   │   │   │  Nginx Alpine      │    │  Spring Boot 3.4           │    │    │    │  │
│  │   │   │   │  Puerto: 80        │    │  Java 21                   │    │    │    │  │
│  │   │   │   │                    │    │  Puerto: 8080              │    │    │    │  │
│  │   │   │   │  React 19 SPA      │    │                            │    │    │    │  │
│  │   │   │   │  MSAL.js           │    │  ┌──────────────────────┐  │    │    │    │  │
│  │   │   │   │  TailwindCSS       │    │  │  Spring Security     │  │    │    │    │  │
│  │   │   │   │                    │    │  │  + JwtDecoder        │  │    │    │    │  │
│  │   │   │   └────────────────────┘    │  │  + JwtAuthConverter  │  │    │    │    │  │
│  │   │   │                             │  │  + RBAC              │  │    │    │    │  │
│  │   │   │                             │  └──────────────────────┘  │    │    │    │  │
│  │   │   │                             │                            │    │    │    │  │
│  │   │   │                             │  ┌──────────────────────┐  │    │    │    │  │
│  │   │   │                             │  │  Controllers         │  │    │    │    │  │
│  │   │   │                             │  │  Services            │  │    │    │    │  │
│  │   │   │                             │  │  Repositories (JPA)  │  │    │    │    │  │
│  │   │   │                             │  └──────────┬───────────┘  │    │    │    │  │
│  │   │   │                             │             │              │    │    │    │  │
│  │   │   │                             └─────────────┼──────────────┘    │    │    │  │
│  │   │   │                                           │                   │    │    │  │
│  │   │   └───────────────────────────────────────────┼───────────────────┘    │    │  │
│  │   │                                               │                        │    │  │
│  │   └───────────────────────────────────────────────┼────────────────────────┘    │  │
│  │                                                   │                             │  │
│  │                                                   │ JDBC (5432)                 │  │
│  │                                                   ▼                             │  │
│  │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│  │   │                         RDS PostgreSQL                                   │   │  │
│  │   │                         vidasalud-db                                     │   │  │
│  │   │                         db.t3.micro | PostgreSQL 15.17                   │   │  │
│  │   │                         SG: vidasalud-rds-sg                             │   │  │
│  │   │                         NO Public Access                                 │   │  │
│  │   │                                                                          │   │  │
│  │   │   Endpoint: vidasalud-db.c2xcy7q094zc.us-east-1.rds.amazonaws.com       │   │  │
│  │   │   Database: vidasalud                                                    │   │  │
│  │   │   Table: appointments (id, patient_name, doctor_name, ...)              │   │  │
│  │   │                                                                          │   │  │
│  │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│  │                                                                                  │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                    ECR                                            │  │
│  │                                                                                   │  │
│  │   ┌─────────────────────────────┐    ┌─────────────────────────────┐             │  │
│  │   │    vidasalud-frontend       │    │      ms-vidasalud-bff       │             │  │
│  │   │    (Docker Image)           │    │      (Docker Image)         │             │  │
│  │   └─────────────────────────────┘    └─────────────────────────────┘             │  │
│  │                                                                                   │  │
│  └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

# 12. FLUJO DE UNA PETICIÓN REAL

## 12.1 Caso: Paciente consulta "Mis Atenciones"

**Acción del usuario:** Click en "Mis Atenciones" en el menú lateral

### Paso 1: Frontend detecta que necesita datos
```typescript
// AppointmentsPage.tsx
useEffect(() => {
  appointmentService.getAppointments().then(setAppointments);
}, []);
```

### Paso 2: Axios interceptor añade el token
```typescript
// api.ts interceptor
const response = await msalInstance.acquireTokenSilent({
  scopes: [ENV.API_SCOPE],
  account: activeAccount,
});
config.headers.Authorization = `Bearer ${response.accessToken}`;
```

**Petición HTTP resultante:**
```http
GET https://nskclbdqa3.execute-api.us-east-1.amazonaws.com/api/appointments
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik...
Content-Type: application/json
```

### Paso 3: API Gateway recibe la petición

**JWT Authorizer verifica:**
```
✓ Header Authorization presente
✓ Formato "Bearer <token>" correcto
✓ Token decodificable (Base64)
✓ Firma válida (verificada con JWKS de Microsoft)
✓ iss == "https://login.microsoftonline.com/877a72d5-.../v2.0"
✓ aud == "693636ac-8558-4324-851a-974c8dce1005"
✓ exp > now (token no expirado)

→ Token válido, continuar
```

### Paso 4: API Gateway hace proxy al BFF
```http
GET http://3.235.125.162:8080/api/appointments
Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik...
```

### Paso 5: Spring Security procesa
```
1. JwtDecoder re-valida el token
2. JwtAuthenticationConverter extrae roles:
   JWT claim "roles": ["Paciente"]
   → authorities: [ROLE_Paciente]
3. SecurityConfig verifica:
   /api/appointments/** requiere hasAnyRole("Admin", "Recepcionista", "Paciente")
   Usuario tiene ROLE_Paciente
   → Autorizado ✓
```

### Paso 6: Controller → Service → Repository
```java
// AppointmentController
@GetMapping
@PreAuthorize("hasAnyRole('Admin', 'Recepcionista', 'Paciente')")
public ResponseEntity<List<AppointmentDto>> getAllAppointments() {
    List<AppointmentDto> appointments = appointmentService.getAllAppointments();
    return ResponseEntity.ok(appointments);
}

// AppointmentService
public List<AppointmentDto> getAllAppointments() {
    List<Appointment> appointments = appointmentRepository.findAll();
    return appointments.stream().map(this::toDto).collect(Collectors.toList());
}

// JPA genera:
// SELECT * FROM appointments
```

### Paso 7: Respuesta
```json
HTTP 200 OK
Content-Type: application/json

[
  {
    "id": "APT-101",
    "patientName": "Carlos Mendoza",
    "doctorName": "Dra. Patricia Vega",
    "specialty": "Medicina General",
    "dateTime": "2026-09-20T09:00:00",
    "status": "PENDING",
    "roomNumber": "Sala 1A",
    "notes": "Control mensual de presión arterial"
  },
  ...
]
```

### Paso 8: Frontend renderiza
React recibe el JSON y actualiza el estado, renderizando las cards de citas.

---

## 12.2 Caso: POST /api/appointments/{id}/confirm

### Como ADMIN (200 OK)

```
1. Admin hace click en "Confirmar" en cita APT-101
2. Frontend envía:
   POST /api/appointments/APT-101/confirm
   Authorization: Bearer <token con roles: ["Admin"]>

3. API Gateway valida JWT → OK
4. Spring Security:
   - JwtAuthConverter extrae: ROLE_Admin
   - @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')") → Admin tiene permiso ✓

5. AppointmentService.confirmAppointment("APT-101"):
   - Busca appointment en DB
   - Cambia status a "CONFIRMED"
   - Guarda en DB

6. Respuesta:
   HTTP 200 OK
   { "id": "APT-101", "status": "CONFIRMED", ... }
```

### Como PACIENTE (403 Forbidden)

```
1. Paciente intenta confirmar APT-101
2. Frontend envía:
   POST /api/appointments/APT-101/confirm
   Authorization: Bearer <token con roles: ["Paciente"]>

3. API Gateway valida JWT → OK (JWT es válido)
4. Spring Security:
   - JwtAuthConverter extrae: ROLE_Paciente
   - @PreAuthorize("hasAnyRole('Admin', 'Recepcionista')") 
   - Paciente NO está en la lista → DENEGADO ✗

5. CustomAccessDeniedHandler responde:
   HTTP 403 Forbidden
   {
     "timestamp": "2026-09-18T...",
     "status": 403,
     "error": "Forbidden",
     "message": "Acceso denegado: rol o privilegio insuficiente para esta operación",
     "path": "/api/appointments/APT-101/confirm"
   }
```

**Diferencia clave:**
- **401 Unauthorized:** No tienes token o el token es inválido
- **403 Forbidden:** Tu token es válido, pero no tienes permiso para esta acción

---

# 13. EVIDENCIAS DE PRUEBAS

## 13.1 Evidencias de Seguridad

| # | Prueba | Resultado Esperado | Evidencia |
|---|--------|-------------------|-----------|
| 1 | Login con MSAL | Redirección a Microsoft, retorno con token | Login funciona, usuario ve dashboard |
| 2 | Logout | Sesión terminada, token eliminado | Redirige a login, sessionStorage vacío |
| 3 | Request sin token | HTTP 401 | `curl` sin Authorization → 401 |
| 4 | Request con token falso | HTTP 401 | Token inventado → 401 |
| 5 | Paciente accede a /api/appointments | HTTP 200 | Datos retornados correctamente |
| 6 | Paciente intenta POST confirm | HTTP 403 | Mensaje "Acceso denegado" |
| 7 | Admin accede a /api/report | HTTP 200 | KPIs retornados |
| 8 | Paciente intenta /api/report | HTTP 403 | Acceso denegado |
| 9 | Auditor accede a /api/audit | HTTP 200 | Logs retornados |
| 10 | Auditor intenta /api/report | HTTP 403 | Solo Admin puede |

## 13.2 Evidencias de Backend

| # | Prueba | Resultado Esperado | Evidencia |
|---|--------|-------------------|-----------|
| 1 | mvn clean test | BUILD SUCCESS | Tests pasan (1/1 con H2) |
| 2 | Health check | PostgreSQL UP | `/actuator/health` muestra db.status=UP |
| 3 | GET /api/appointments | Datos de RDS | Retorna APT-101, APT-102, APT-103 |
| 4 | POST confirm APT-101 | Status cambia | De PENDING a CONFIRMED |
| 5 | Persistencia | Cambio persiste | Después de reiniciar BFF, sigue CONFIRMED |

## 13.3 Evidencias de AWS

| # | Prueba | Resultado Esperado | Evidencia |
|---|--------|-------------------|-----------|
| 1 | EC2 running | Status: running | AWS Console o `aws ec2 describe-instances` |
| 2 | RDS available | Status: available | AWS Console muestra "available" |
| 3 | API Gateway funcional | Respuestas correctas | Peticiones llegan al BFF |
| 4 | Elastic IP asociada | IP fija | 3.235.125.162 no cambia |
| 5 | ECR con imágenes | Imágenes presentes | Listado muestra ambos repos |

## 13.4 Evidencias de Persistencia

| # | Prueba | Resultado Esperado | Evidencia |
|---|--------|-------------------|-----------|
| 1 | Tabla appointments existe | Tabla con columnas | `\d appointments` en psql |
| 2 | Datos iniciales | 3 registros | APT-101, APT-102, APT-103 |
| 3 | Modificación persiste | Cambio guardado | Confirmar cita → recargar → sigue confirmada |
| 4 | Conexión desde EC2 | Conexión exitosa | BFF conecta y health muestra UP |

---

# 14. PREGUNTAS FRECUENTES DEL PROFESOR

## Preguntas sobre Arquitectura General

### 1. ¿Por qué usamos API Gateway?
**Respuesta:** API Gateway proporciona:
- **Terminación SSL gratuita:** HTTPS sin gestionar certificados
- **Validación de JWT:** Rechaza tokens inválidos antes de llegar al BFF
- **Rate limiting:** Protección contra abuso (configurable)
- **CORS centralizado:** Configuración en un solo lugar
- **Logging:** Métricas de uso de la API

### 2. ¿Por qué usamos un BFF (Backend For Frontend)?
**Respuesta:** El BFF actúa como intermediario entre el frontend y los servicios backend. Sus beneficios son:
- **Seguridad:** El frontend nunca habla directamente con la base de datos
- **Agregación:** Puede combinar datos de múltiples fuentes
- **Transformación:** Adapta los datos al formato que el frontend necesita
- **Validación adicional:** Segunda capa de validación de JWT y roles
- **Ocultación:** La base de datos está completamente oculta de Internet

### 3. ¿Por qué JWT y no sesiones tradicionales?
**Respuesta:** JWT es ideal para arquitecturas cloud porque:
- **Stateless:** El servidor no necesita guardar sesiones (escalabilidad horizontal)
- **Autocontenido:** El token contiene toda la información necesaria (roles, usuario)
- **Estándar:** Interoperable entre servicios, lenguajes y plataformas
- **Delegación:** La autenticación la hace un tercero (Azure AD) especializado

### 4. ¿Por qué validar el JWT dos veces (API Gateway y Spring)?
**Respuesta:** Defensa en profundidad:
- **API Gateway:** Protege la infraestructura AWS, rechaza peticiones inválidas temprano
- **Spring Security:** Extrae información del token (roles), permite lógica de autorización compleja, funciona si alguien accede directamente al puerto 8080

### 5. ¿Qué pasaría si alguien llama directamente al BFF (puerto 8080)?
**Respuesta:** Spring Security lo rechazaría si:
- No tiene token → 401
- Token inválido → 401
- Token válido pero sin rol → 403

El BFF NO confía ciegamente en las peticiones; siempre valida el JWT.

## Preguntas sobre Seguridad

### 6. ¿Qué diferencia hay entre 401 y 403?
**Respuesta:**
- **401 Unauthorized:** "No sé quién eres" - Falta token, token expirado, token inválido
- **403 Forbidden:** "Sé quién eres pero no puedes hacer esto" - Token válido pero sin permiso

### 7. ¿Qué ocurre si alguien modifica su JWT para añadirse el rol Admin?
**Respuesta:** El token quedaría inválido porque:
- El JWT está firmado criptográficamente (RS256)
- Al modificar el payload, la firma ya no coincide
- El validador verifica la firma contra las claves públicas de Microsoft
- Resultado: 401 Unauthorized

### 8. ¿Qué pasa si el token expira mientras el usuario está usando la app?
**Respuesta:** 
- MSAL.js intenta renovar el token silenciosamente (acquireTokenSilent)
- Si no puede (sesión expirada en Azure AD), redirige al login
- El usuario debe autenticarse nuevamente

### 9. ¿Por qué RDS no es público?
**Respuesta:** Por seguridad:
- Datos de pacientes son sensibles (cumplimiento HIPAA/GDPR)
- Reduce superficie de ataque (no expuesto a Internet)
- Best practice de AWS: bases de datos siempre privadas
- El BFF es el único que necesita acceso

### 10. ¿Cómo se protegen las credenciales de la base de datos?
**Respuesta:**
- Variables de entorno (DB_URL, DB_USERNAME, DB_PASSWORD)
- No están en el código ni en Git
- Se pasan al contenedor Docker en tiempo de ejecución
- El .env no se sube al repositorio (.gitignore)

## Preguntas sobre Tecnologías

### 11. ¿Por qué PostgreSQL y no MySQL?
**Respuesta:**
- Mejor soporte de tipos de datos (JSON, arrays)
- Más estándares SQL
- Excelente integración con Spring Data JPA
- Usado ampliamente en la industria
- Free Tier disponible en RDS

### 12. ¿Qué hace JPA/Hibernate?
**Respuesta:** JPA (Java Persistence API) es un estándar para ORM (Object-Relational Mapping):
- Mapea clases Java a tablas SQL (@Entity)
- Genera queries SQL automáticamente (findAll, findById, save)
- Maneja conexiones y transacciones
- Evita escribir SQL manualmente en la mayoría de casos

### 13. ¿Qué hace el Repository en Spring?
**Respuesta:** El Repository es una interfaz que:
- Extiende JpaRepository
- Proporciona métodos CRUD sin escribir código
- Permite definir queries personalizados por convención de nombres
- Ejemplo: `findByStatus(String status)` genera `SELECT * FROM ... WHERE status = ?`

### 14. ¿Qué hace ECR?
**Respuesta:** Amazon Elastic Container Registry:
- Almacena imágenes Docker
- Similar a Docker Hub pero privado y dentro de AWS
- Integrado con IAM para control de acceso
- La EC2 descarga imágenes de aquí para ejecutar contenedores

### 15. ¿Por qué usar contenedores Docker?
**Respuesta:**
- **Consistencia:** "Funciona en mi máquina" → Funciona igual en EC2
- **Aislamiento:** Frontend y BFF corren independientes
- **Portabilidad:** La misma imagen corre en cualquier lugar
- **Versionado:** Cada imagen tiene un tag único

## Preguntas sobre Frontend

### 16. ¿Cómo funciona MSAL.js?
**Respuesta:** MSAL (Microsoft Authentication Library):
- Maneja el flujo OAuth 2.0 con Azure AD
- Almacena tokens en sessionStorage
- Renueva tokens automáticamente antes de expirar
- Proporciona la cuenta activa y sus claims

### 17. ¿Por qué el frontend puede correr en localhost pero conectarse a AWS?
**Respuesta:**
- CORS está configurado para permitir `localhost:5173` y `localhost:3000`
- Azure AD tiene configurados estos URIs de redirección
- El frontend solo necesita poder alcanzar API Gateway (Internet pública)

### 18. ¿Qué pasa si el frontend intenta llamar directamente al BFF sin pasar por API Gateway?
**Respuesta:** Funcionaría técnicamente (mismo resultado), pero:
- No tendría HTTPS (menos seguro)
- No tendría la primera capa de validación JWT
- No tendría el logging de API Gateway
- En producción real, el puerto 8080 debería estar cerrado a Internet

## Preguntas sobre AWS

### 19. ¿Por qué usar Elastic IP?
**Respuesta:** Sin Elastic IP:
- Cada reinicio de EC2 asigna una nueva IP pública
- Tendrías que actualizar: API Gateway, CORS, Azure AD redirect URIs
- Con Elastic IP, la IP es fija y sobrevive reinicios

### 20. ¿Qué son los Security Groups?
**Respuesta:** Son firewalls virtuales de AWS:
- Definen qué tráfico entra y sale de un recurso
- Reglas basadas en puerto, protocolo y origen/destino
- Pueden referenciar otros Security Groups (como vidasalud-rds-sg que permite conexiones desde vidasalud-sg)

### 21. ¿Qué pasa si la EC2 se apaga?
**Respuesta:**
- Los contenedores se detienen
- Los datos en RDS permanecen (está separado)
- Al encender EC2, hay que iniciar los contenedores manualmente (o configurar auto-start)
- La Elastic IP se mantiene asociada

### 22. ¿Cómo escalaría esta arquitectura si tuviera miles de usuarios?
**Respuesta:** Para escalar:
- **EC2:** Usar Auto Scaling Group con Load Balancer
- **RDS:** Aumentar instancia o usar Read Replicas
- **API Gateway:** Escala automáticamente
- **Contenedores:** Migrar a ECS o EKS para orquestación

---

# 15. RESUMEN PARA EXPOSICIÓN ORAL (2-3 minutos)

> "Esta es la arquitectura AWS de VidaSalud, un sistema de gestión de citas médicas que implementa autenticación OAuth 2.0 con Azure AD y control de acceso basado en roles.
>
> **El flujo comienza** cuando el usuario accede al frontend, una SPA React servida por Nginx en una EC2. Si no está autenticado, MSAL.js lo redirige a Microsoft Entra ID donde ingresa sus credenciales. Azure AD genera un JWT Access Token que contiene los roles del usuario.
>
> **Cuando el frontend necesita datos**, como las citas médicas, envía la petición a AWS API Gateway con el token en el header Authorization. API Gateway tiene un JWT Authorizer que valida la firma, el issuer y la audiencia del token. Si el token es inválido, rechaza con 401 sin llegar al backend.
>
> **Si el token es válido**, API Gateway hace proxy HTTP al BFF Spring Boot corriendo en la misma EC2 en puerto 8080. Spring Security realiza una segunda validación del JWT y extrae los roles. Por ejemplo, el claim `roles: ["Paciente"]` se convierte en la autoridad `ROLE_Paciente`.
>
> **El sistema implementa RBAC** donde cada endpoint tiene roles permitidos. Un Paciente puede ver citas pero no confirmarlas. Solo Admin y Recepcionista pueden confirmar. Si alguien intenta una acción sin el rol correcto, recibe 403 Forbidden.
>
> **Para la persistencia**, usamos RDS PostgreSQL configurado como privado, solo accesible desde la EC2. El BFF usa Spring Data JPA para mapear la entidad Appointment a la tabla appointments. Los datos son reales y persisten entre reinicios.
>
> **La seguridad se implementa en capas**: Azure AD autentica, API Gateway valida tokens, Spring Security verifica roles, y RDS está aislado en la VPC. Las imágenes Docker se almacenan en ECR y se despliegan en EC2.
>
> En resumen: autenticación delegada a Azure AD, doble validación de JWT, RBAC granular por endpoint, y base de datos privada. Todo corriendo en AWS con EC2, API Gateway, RDS y ECR."

---

# ANEXO: URLs y Endpoints Importantes

## URLs de Producción

| Recurso | URL |
|---------|-----|
| Frontend | http://3.235.125.162/ |
| API Gateway | https://nskclbdqa3.execute-api.us-east-1.amazonaws.com |
| BFF directo (debug) | http://3.235.125.162:8080/ |
| Health Check | http://3.235.125.162:8080/actuator/health |

## Endpoints de la API

| Endpoint | Método | Roles | Descripción |
|----------|--------|-------|-------------|
| /api/appointments | GET | Admin, Recepcionista, Paciente | Lista todas las citas |
| /api/appointments/{id} | GET | Admin, Recepcionista, Paciente | Obtiene una cita |
| /api/appointments/{id}/confirm | POST | Admin, Recepcionista | Confirma una cita |
| /api/catalog/specialties | GET | Admin, Recepcionista | Lista especialidades |
| /api/report/kpis | GET | Admin | KPIs gerenciales |
| /api/audit/logs | GET | Admin, Auditor | Logs de auditoría |
| /api/me | GET | Cualquier autenticado | Info del usuario actual |
| /actuator/health | GET | Público | Estado del sistema |

## Azure AD Configuration

| Parámetro | Valor |
|-----------|-------|
| Tenant ID | 877a72d5-ba5e-45e0-8b5c-ebb0268f993a |
| Client ID | 693636ac-8558-4324-851a-974c8dce1005 |
| Authority | https://login.microsoftonline.com/877a72d5-ba5e-45e0-8b5c-ebb0268f993a |
| API Scope | api://693636ac-8558-4324-851a-974c8dce1005/access_as_user |

---

*Documento generado para defensa oral EP1 - Cloud Native I (DSY1107)*
*VidaSalud - Sistema de Gestión de Citas Médicas*
