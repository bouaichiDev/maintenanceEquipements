# Documentation — SaaS de gestion de maintenance des équipements

API REST Spring Boot **multi-tenant (SaaS)** de gestion de maintenance des équipements
(salles / laboratoires), inspirée du rapport de projet de fin de module.

N'importe qui peut créer un compte : l'inscription crée une **nouvelle organisation** dont
l'inscrit devient l'**administrateur**. Chaque organisation ne voit que ses propres données.

---

## 1. Démarrage rapide

### Prérequis
- Java 17, MySQL 8 démarré sur `localhost:3306` (la base `maintenance_db` est créée automatiquement).
- Identifiants MySQL par défaut : `root` sans mot de passe (modifiable dans `application.properties`).

### Lancer
```bash
./mvnw spring-boot:run
```
- API : http://localhost:8081
- Documentation interactive Swagger : http://localhost:8081/swagger-ui.html
  (bouton **Authorize** pour coller le token JWT)

### Variables d'environnement (optionnelles)
| Variable | Défaut | Rôle |
|----------|--------|------|
| `JWT_SECRET` | clé de dev | Secret de signature JWT |
| `JWT_EXPIRATION` | `86400000` (24h) | Durée de validité du token (ms) |
| `APP_UPLOAD_DIR` | `uploads` | Dossier de stockage des photos |

### Comptes de démonstration (créés au premier démarrage)
| Compte | Mot de passe | Rôle | Organisation |
|--------|--------------|------|--------------|
| `superadmin` | `admin123` | SUPER_ADMIN | (aucune) |
| `admin` | `1234` | ADMIN | ENS Marrakech |
| `tech` | `1234` | TECH | ENS Marrakech |
| `user` | `1111` | USER | ENS Marrakech |

---

## 2. Rôles et sécurité

| Rôle | Portée | Capacités |
|------|--------|-----------|
| `SUPER_ADMIN` | Plateforme | Supervise toutes les organisations, suspend/active, stats globales |
| `ADMIN` | Organisation | Gère utilisateurs, équipements, assignation des tickets, dashboard, audit |
| `TECH` | Organisation | Traite les tickets, change les statuts, dashboard |
| `USER` | Organisation | Crée et suit ses propres tickets |

- **Authentification** : JWT (24h) signé HS256. Le token porte `username`, `organizationId` et `role`.
- **Mots de passe** : hachés avec BCrypt.
- **Isolation multi-tenant** : chaque requête est filtrée par l'organisation de l'utilisateur
  connecté (`CurrentUserProvider`). Le `SUPER_ADMIN` n'est rattaché à aucune organisation.
- **CORS** activé (pour les fronts React / mobile).

---

## 3. Schéma de la base de données

Tables (générées par Hibernate, `ddl-auto=update`) :

| Table | Rôle |
|-------|------|
| `organizations` | Clients SaaS (établissements) |
| `users` | Comptes, rattachés à une organisation (sauf SUPER_ADMIN) |
| `roles` / `user_roles` | Rôles (relation ManyToMany) |
| `equipments` | Parc d'équipements, par organisation |
| `maintenance_tickets` | Tickets de panne, par organisation |
| `ticket_logs` | Historique des actions sur un ticket |
| `activity_logs` | Journal d'audit global, par organisation |

Relations : `users`/`equipments`/`maintenance_tickets`/`activity_logs` → `organizations`.
Un ticket référence un `equipment`, un `user` (déclarant), un `technician` (nullable).

---

## 4. Endpoints de l'API

### Authentification — `/api/auth` (public)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Crée une organisation + un compte ADMIN, renvoie un JWT |
| POST | `/api/auth/login` | Authentifie, renvoie un JWT enrichi |

### Équipements — `/api/equipments`
| Méthode | Endpoint | Accès |
|---------|----------|-------|
| GET | `/api/equipments?status=&room=&page=&size=&sort=` | Authentifié (org) |
| GET | `/api/equipments/{id}` | Authentifié (org) |
| POST | `/api/equipments` | ADMIN |
| PUT | `/api/equipments/{id}` | ADMIN |
| DELETE | `/api/equipments/{id}` | ADMIN |

### Tickets — `/api/tickets`
| Méthode | Endpoint | Accès |
|---------|----------|-------|
| GET | `/api/tickets?status=&priority=&search=&page=&size=&sort=` | Authentifié (USER : ses tickets ; TECH/ADMIN : tous) |
| GET | `/api/tickets/{id}` | Authentifié (org) |
| GET | `/api/tickets/{id}/logs` | Authentifié (org) — timeline du ticket |
| POST | `/api/tickets` | Authentifié — crée un ticket (SLA auto) |
| PATCH | `/api/tickets/{id}/status` | TECH / ADMIN |
| PATCH | `/api/tickets/{id}/assign` | ADMIN |
| POST | `/api/tickets/{id}/photo` (multipart `file`) | Authentifié (org) |

### Tableau de bord — `/api/dashboard`
| Méthode | Endpoint | Accès | Contenu |
|---------|----------|-------|---------|
| GET | `/api/dashboard/stats` | TECH / ADMIN | Statistiques globales de l'organisation |
| GET | `/api/dashboard/my` | Authentifié | Dashboard personnel adapté au rôle |

**Dashboards par rôle :**
- `USER` → `/api/dashboard/my` : stats de **ses** tickets (créés : total, ouverts, en cours, résolus, fermés, en retard).
- `TECH` → `/api/dashboard/my` : ses tickets créés + ses tickets **assignés** (total, en cours, en retard) + nombre de tickets ouverts **non assignés** à traiter.
- `ADMIN` → `/api/dashboard/stats` (vue org complète) + `/api/dashboard/my`.
- `SUPER_ADMIN` → `/api/admin/stats` + `/api/admin/organizations` (vue plateforme).

### Historique / audit — `/api/activity`
| Méthode | Endpoint | Accès |
|---------|----------|-------|
| GET | `/api/activity?type=&page=&size=&sort=` | ADMIN / TECH — fil d'activité de l'organisation (filtre par type : TICKET/EQUIPMENT/USER/AUTH) |

### Administration d'organisation — `/api/org/users` (ADMIN)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/org/users?role=&active=&search=&page=&size=&sort=` | Liste paginée/filtrée des comptes de l'organisation |
| POST | `/api/org/users` | Crée un compte (rôle TECH ou USER) |
| PATCH | `/api/org/users/{id}/role` | Change le rôle (TECH/USER) |
| PATCH | `/api/org/users/{id}/status` | Active / désactive un compte |

### Console plateforme — `/api/admin` (SUPER_ADMIN)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/admin/organizations?active=&search=&page=&size=&sort=` | Liste paginée/filtrée des organisations + stats (users/tickets/équipements) |
| PATCH | `/api/admin/organizations/{id}/status` | Suspend / réactive une organisation |
| GET | `/api/admin/stats` | Statistiques globales du SaaS |

---

## 5. Règles métier

### SLA (échéance automatique des tickets)
À la création, `dueDate` est calculée selon la priorité :
| Priorité | Délai |
|----------|-------|
| HIGH | 24 heures |
| MEDIUM | 72 heures |
| LOW | 7 jours |
Un ticket non résolu/fermé dont `dueDate` est dépassée est marqué `overdue` (et compté dans le dashboard).

### Traçabilité
- Chaque action sur un ticket (création, changement de statut, assignation, photo) crée un `TicketLog`.
- Chaque action significative (auth, équipements, tickets, utilisateurs) crée un `ActivityLog`
  consultable via `/api/activity`.

### Statuts et priorités
- `TicketStatus` : `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`
- `Priority` : `LOW`, `MEDIUM`, `HIGH`
- `EquipmentStatus` : `OPERATIONAL`, `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`

---

## 6. Temps réel (WebSocket + STOMP)

- Endpoint de connexion : `ws://localhost:8081/ws` (SockJS).
- Authentification : header STOMP `Authorization: Bearer <token>` à la connexion (CONNECT).
- Topics diffusés (cloisonnés par organisation) :
  | Topic | Contenu |
  |-------|---------|
  | `/topic/org/{organizationId}/tickets` | Création / changement de statut / assignation / photo |
  | `/topic/org/{organizationId}/dashboard` | Signal de rafraîchissement des statistiques |
  | `/topic/org/{organizationId}/activity` | Fil d'activité en direct |
- Format des messages : `{ "type": "...", "payload": {...} }`.

> Remarque : l'authentification WebSocket est faite à la connexion. L'autorisation par topic
> (empêcher un abonnement au topic d'une autre organisation) peut être ajoutée comme amélioration.

---

## 7. Structure du code

```
config/        SecurityConfig, WebConfig, OpenApiConfig, WebSocketConfig, WebSocketAuthChannelInterceptor, DatabaseInitializer
entities/      Organization, User, Role, Equipment, Ticket, TicketLog, ActivityLog
entities/enums Priority, TicketStatus, EquipmentStatus
repositories/  Organization/User/Role/Equipment/Ticket/TicketLog/ActivityLog repositories
security/      CurrentUserProvider (résolution du tenant courant)
dto/           Requêtes/réponses + ApiError + mappers statiques
services/      AuthService, EquipmentService, TicketService, DashboardService, ActivityService,
               AuditService, OrgUserService, AdminService, FileStorageService, UserService, CustomUserDetailsService
realtime/      RealtimeEventPublisher
jwt/           JwtUtil, JwtAuthorizationFilter
web/           AuthController, EquipmentController, TicketController, DashboardController,
               ActivityController, OrgUserController, AdminController, UserController, TestController
exceptions/    GlobalExceptionHandler + exceptions métier
```

### Conventions techniques
- Aucun commentaire dans le code (sur demande).
- Timestamps automatiques (`@CreationTimestamp` / `@UpdateTimestamp`).
- Associations en `FetchType.LAZY`, mapping vers DTO dans des méthodes transactionnelles.
- Validation des entrées via Bean Validation (`@Valid`), erreurs renvoyées en JSON structuré (`ApiError`).
- Pagination Spring Data (`Pageable`) sur les listes.

---

## 8. Tests

- `mvnw test` exécute un test de chargement de contexte avec une base **H2 en mémoire**
  (`src/test/resources/application.properties`) — valide le câblage complet sans MySQL.

---

## 9. Blocs livrés

1. Configuration + dépendances (validation, Swagger, WebSocket).
2. Infrastructure multi-tenant + repositories + exceptions.
3. Authentification & inscription SaaS + JWT enrichi.
4. Domaine métier (équipements, tickets, dashboard, SLA, photo, assignation).
5. Qualité API (gestion globale des erreurs, Swagger JWT, pagination, validation).
6. Historique / audit (ActivityLog + `/api/activity`).
7. Temps réel WebSocket + STOMP (topics par organisation).
8. Administration (admin d'organisation + console super-admin plateforme).
