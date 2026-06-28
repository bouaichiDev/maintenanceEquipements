# Backend — Maintenance des Équipements (SaaS)

Multi-tenant equipment-maintenance management REST API. Organizations sign up, manage their equipment park, and track maintenance tickets through a full lifecycle, with JWT-secured access, role-based permissions, real-time notifications, and an audit trail.

---

## 1. Tech Stack

| Concern | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.15 |
| Build | Maven (`spring-boot-maven-plugin`), Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL (runtime), H2 (tests) |
| Security | Spring Security + JWT (`io.jsonwebtoken` jjwt 0.11.5) |
| Real-time | Spring WebSocket + STOMP (SockJS fallback) |
| API docs | springdoc OpenAPI / Swagger UI 2.7.0 |
| Validation | `spring-boot-starter-validation` (Jakarta Bean Validation) |
| Boilerplate | Lombok |

- **Group / artifact:** `ma.fstg.security` / `spring-jwt-api` (v `0.0.1-SNAPSHOT`)
- **Base package:** `ma.fstg.security.spring_jwt_api`

---

## 2. Configuration (`src/main/resources/application.properties`)

| Property | Value / default |
|---|---|
| `server.port` | **8081** |
| `spring.application.name` | `spring-jwt-api` |
| `spring.datasource.url` | `jdbc:mysql://127.0.0.1:3306/maintenance_db` (`createDatabaseIfNotExist=true`, UTC) |
| `spring.datasource.username` | `root` |
| `spring.datasource.password` | *(empty)* |
| `spring.jpa.hibernate.ddl-auto` | `update` (schema auto-managed by Hibernate) |
| `spring.jpa.show-sql` | `true` |
| `jwt.secret` | env `JWT_SECRET`, default `MySuperSecretKeyForJwtAuthentication123456` |
| `jwt.expiration` | env `JWT_EXPIRATION`, default `86400000` ms (24 h) |
| `spring.servlet.multipart.max-file-size` | `5MB` |
| `app.upload.dir` | env `APP_UPLOAD_DIR`, default `uploads` |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` |

> **Production note:** the JWT secret and DB credentials default to dev values and should be overridden via environment variables before deployment.

### Run

```bash
cd api
./mvnw spring-boot:run        # or mvnw.cmd on Windows
```

- API base: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8081/v3/api-docs`

Requires a running MySQL on `127.0.0.1:3306` (the DB is auto-created).

---

## 3. Architecture

Standard layered Spring Boot structure under `ma.fstg.security.spring_jwt_api`:

```
web/          REST controllers (@RestController)
services/     Business logic (@Service, @Transactional)
repositories/ Spring Data JPA interfaces
entities/     JPA @Entity classes  (+ entities/enums/)
dto/          Request/response objects
config/       Security, WebSocket, Web, OpenAPI, DB seeding
jwt/          JWT utility + authorization filter
security/     CurrentUserProvider (tenant/role helper)
realtime/     WebSocket event publisher
exceptions/   Custom exceptions + global handler
```

**Multi-tenancy** is the central design principle: every domain row belongs to an `Organization`, and nearly every repository query is scoped by `organization_id`. `CurrentUserProvider.getCurrentOrganizationId()` resolves the caller's org from the JWT/security context and is the enforcement point for tenant isolation.

---

## 4. Data Model

All entities use Lombok and a `Long id` (`@GeneratedValue(IDENTITY)`).

### Entities

| Entity | Table | Key fields | Relationships |
|---|---|---|---|
| **User** | `users` | `username`, `password`, `active` | `@ManyToOne` → Organization; `@ManyToMany(EAGER)` Roles via `user_roles` |
| **Role** | `roles` | `name` | — |
| **Organization** | `organizations` | `name`, `active`, `createdAt` | tenant root |
| **Equipment** | `equipments` | `name`, `room`, `type`, `status` (enum), `createdAt` | `@ManyToOne` → Organization |
| **Ticket** | `maintenance_tickets` | `description`, `priority` (enum), `status` (enum), `photoUrl`, `resolutionComment`, `dueDate`, `createdAt`, `updatedAt` | `@ManyToOne` → Equipment, User (reporter), User (technician/assignee), Organization |
| **TicketLog** | `ticket_logs` | `action`, `oldValue`, `newValue`, `createdAt` | `@ManyToOne` → Ticket, User |
| **ActivityLog** | `activity_logs` | `action`, `entityType`, `entityId`, `details`, `createdAt` | `@ManyToOne` → Organization, User (polymorphic ref via `entityType`+`entityId`) |

All relationships are unidirectional `@ManyToOne` (no inverse `@OneToMany` collections), plus the one `@ManyToMany` on User.

### Enums (`entities/enums`)

- **Priority:** `LOW`, `MEDIUM`, `HIGH`
- **TicketStatus:** `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- **EquipmentStatus:** `OPERATIONAL`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`

### Relationship overview

```
Organization 1───* User ──*─* Role
     │ 1            │ (reporter)
     ├──* Equipment │
     ├──* Ticket *──┤ (technician)
     │       └──* Equipment
     ├──* ActivityLog
     └ (Ticket 1──* TicketLog)
```

---

## 5. Security

### JWT authentication flow

- **Token issuance:** `JwtUtil` builds an HS256 JWT signed with `jwt.secret`. Claims: `sub` = username, custom `organizationId`, custom `role`, plus issued-at/expiration (`jwt.expiration`).
- **Request filter:** `JwtAuthorizationFilter` (a `OncePerRequestFilter`, registered before `UsernamePasswordAuthenticationFilter`) reads the `Authorization: Bearer <token>` header, validates the token, loads the user via `CustomUserDetailsService`, and sets the authentication into the `SecurityContext`.
- **User loading:** `CustomUserDetailsService` looks up `User` by username, maps each `Role.name` to a `SimpleGrantedAuthority`, and uses the `active` flag for `enabled`.
- **Passwords:** `BCryptPasswordEncoder`.
- **Session policy:** STATELESS; CSRF disabled.

### Roles

`SUPER_ADMIN` › `ADMIN` › `TECH` › `USER` (stored as `ROLE_*` authorities). `CurrentUserProvider` exposes `isSuperAdmin/isAdmin/isTechnician` and resolves the current user + org.

### Route access (`SecurityConfig`)

| Pattern | Access |
|---|---|
| `/api/auth/**` | Public |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Public |
| `/ws/**` | Public (handshake; auth at STOMP CONNECT) |
| `/uploads/**` | Public (static files) |
| `/api/admin/**` | `hasRole('SUPER_ADMIN')` |
| everything else | Authenticated |

Method-level security (`@EnableMethodSecurity`) adds finer `@PreAuthorize` checks per endpoint (see API table).

### CORS

Applied to `/**`: all origin patterns, methods `GET/POST/PUT/PATCH/DELETE/OPTIONS`, all headers, **credentials allowed**.

---

## 6. REST API

All paths are prefixed by host `http://localhost:8081`. Unless noted, endpoints require a valid JWT; org scoping is automatic.

### Auth — `/api/auth` (public)
| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/register` | `RegisterRequest` | Creates a new Organization + its ADMIN user, returns `AuthResponse` (JWT) |
| POST | `/login` | `LoginRequest` | Returns `AuthResponse` (JWT); blocked if org suspended |

### User — `/api/user`
| Method | Path | Notes |
|---|---|---|
| GET | `/profile` | Current user's `UserProfileDto` |

### Equipment — `/api/equipments`
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/` | any auth | List, filters: `status`, `room`, paged (`Page<EquipmentDto>`) |
| GET | `/{id}` | any auth | Single equipment |
| POST | `/` | ADMIN | Create (201) |
| PUT | `/{id}` | ADMIN | Update |
| DELETE | `/{id}` | ADMIN | Delete (204) |

### Tickets — `/api/tickets`
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/` | any auth | List, filters: `status`, `priority`, `search`, paged |
| GET | `/{id}` | any auth | Single ticket |
| GET | `/{id}/logs` | any auth | Ticket history (`List<TicketLogDto>`) |
| POST | `/` | any auth | Create ticket (201), body `CreateTicketRequest` |
| PATCH | `/{id}/status` | TECH, ADMIN | Update status, body `UpdateStatusRequest` |
| PATCH | `/{id}/assign` | ADMIN | Assign technician, body `AssignTicketRequest` |
| POST | `/{id}/photo` | any auth | Upload photo (multipart `file`) → attaches URL |

### Activity — `/api/activity`
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/` | ADMIN, TECH | Org audit log, filter `type`, paged (size 20, newest first) |

### Org Users — `/api/org/users` (entire controller: ADMIN)
| Method | Path | Notes |
|---|---|---|
| GET | `/` | List org users, filters: `role`, `active`, `search`, paged |
| POST | `/` | Create user in org (201), body `CreateUserRequest` |
| PATCH | `/{id}/role` | Change role, body `UpdateUserRoleRequest` |
| PATCH | `/{id}/status` | Activate/deactivate, body `UpdateActiveRequest` |

### Dashboard — `/api/dashboard`
| Method | Path | Role | Notes |
|---|---|---|---|
| GET | `/stats` | TECH, ADMIN | Org-wide `DashboardStatsDto` (counts by status/priority, equipment count, overdue, top-5 equipment) |
| GET | `/my` | any auth | Personal `PersonalDashboardDto` (created/assigned tickets, overdue) |

### Admin (platform) — `/api/admin` (SUPER_ADMIN only, via SecurityConfig)
| Method | Path | Notes |
|---|---|---|
| GET | `/organizations` | List orgs, filters `active`/`search`, paged |
| GET | `/organizations/{id}` | Org summary |
| PATCH | `/organizations/{id}/status` | Suspend/activate org |
| GET | `/organizations/{id}/users` | Users of an org |
| GET | `/organizations/{id}/tickets` | Tickets of an org, paged |
| GET | `/stats` | Platform-wide `PlatformStatsDto` |
| GET | `/activity` | Recent platform activity |

> `TestController` exists but is an inert placeholder (no mappings).

---

## 7. Services (business logic)

| Service | Responsibility |
|---|---|
| **AuthService** | Register (creates org + ADMIN user), login (with suspended-org check), JWT issuance, primary-role resolution; audits AUTH events |
| **UserService** | Current-user profile lookup → `UserProfileDto` |
| **EquipmentService** | Org-scoped equipment CRUD; defaults status to `OPERATIONAL`; audits EQUIPMENT events |
| **TicketService** | Ticket lifecycle: create, list/filter, status update, technician assignment, photo attach, history logs |
| **OrgUserService** | Admin management of users within an org (list, create, role, active status) |
| **DashboardService** | Read-only analytics: org stats + personal dashboard, overdue calc, top-equipment tally |
| **ActivityService** | Paged org activity-log retrieval |
| **AdminService** | Platform/super-admin operations across all orgs + platform stats |
| **AuditService** | Central `record(...)` → persists `ActivityLog` and publishes it over WebSocket |
| **FileStorageService** | Image upload: validates `image/*`, sanitizes filename, path-traversal guard, stores under `app.upload.dir`, returns `/uploads/{file}` URL |
| **CustomUserDetailsService** | Spring Security `UserDetailsService` implementation |

### Repositories

All `JpaRepository<Entity, Long>`; several add `JpaSpecificationExecutor`. Queries are pervasively scoped by `Organization_Id` (multi-tenancy enforced at the data layer): User, Role, Organization, Equipment, Ticket, TicketLog, ActivityLog.

---

## 8. Real-time (WebSocket / STOMP)

- **Endpoint:** `/ws` (SockJS fallback, all origin patterns).
- **Broker:** in-memory simple broker, destination prefix `/topic`; app prefix `/app`.
- **Auth:** `WebSocketAuthChannelInterceptor` validates the `Authorization: Bearer` header on the STOMP `CONNECT` frame and attaches the user as session principal.
- **Topics (per organization):**
  - `/topic/org/{orgId}/tickets` — ticket events
  - `/topic/org/{orgId}/dashboard` — `DASHBOARD_REFRESH`
  - `/topic/org/{orgId}/activity` — `ACTIVITY`
- **Message envelope:** `RealtimeMessage(String type, Object payload)`.
- Published by `RealtimeEventPublisher` (no-op when org id is null).

---

## 9. File Uploads

- Multipart, max **5 MB**.
- Stored in `app.upload.dir` (default `./uploads`), served publicly at `/uploads/**` (mapped in `WebConfig`).
- Only `image/*` accepted; filenames sanitized + timestamp-prefixed; path-traversal protected.

---

## 10. Error Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) returns a uniform `ApiError` body (`status`, `error`, `message`, `timestamp`, optional `fieldErrors`):

| Exception | HTTP |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BadRequestException` | 400 |
| `AccessDeniedException` (custom + Spring) | 403 (`"Accès refusé"`) |
| `MethodArgumentNotValidException` | 400 (`fieldErrors` map, `"Validation échouée"`) |
| Any other `Exception` | 500 |

User-facing messages are in **French**.

---

## 11. Database Seeding (`DatabaseInitializer`)

A `CommandLineRunner` runs at startup: idempotently creates the four roles, then (only if `superadmin` doesn't exist) seeds a demo org and users with BCrypt-hashed passwords.

| Username | Password | Organization | Role |
|---|---|---|---|
| `superadmin` | `admin123` | — | SUPER_ADMIN |
| `admin` | `1234` | ENS Marrakech | ADMIN |
| `tech` | `1234` | ENS Marrakech | TECH |
| `user` | `1111` | ENS Marrakech | USER |

> **Dev-only credentials** — do not ship to production.

---

## 12. API Documentation (OpenAPI / Swagger)

- `OpenApiConfig`: title *"API Maintenance des équipements (SaaS)"*, v1.0, global `bearerAuth` security scheme (HTTP bearer, JWT).
- Swagger UI: `/swagger-ui.html` — use the **Authorize** button to paste a JWT and call protected endpoints.
