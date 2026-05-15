# Stoplight Classroom — Plan Specification

## 1. Overview

Stoplight Classroom is a digital implementation of the stoplight active learning method. Teachers create live sessions where students provide real-time comprehension feedback (Green/Yellow/Red) and receive activity-mode notifications. The system tracks statistics at session, course, and lifetime levels.

## 2. Goals

- Provide real-time, visual comprehension feedback from students to teachers.
- Allow teachers to broadcast activity modes (Group / Partner / Silent-Teacher Talk) to students.
- Support multiple concurrent teacher sessions with isolated student groups.
- Expose a RESTful API to enable future native clients (macOS, Android).
- Persist per-session, per-course, and lifetime statistics.
- Enforce authentication and role-based access for teachers and administrators.
- Deploy to a cloud web server.

## 3. Actors & Roles

| Role          | Description |
|---------------|-------------|
| **Admin**     | Manages user accounts (create, delete, modify). Has full system access. |
| **Teacher**   | Creates/manages sessions and courses. Views live dashboards and statistics. |
| **Student**   | Joins a session (no account required — session code entry). Submits stoplight responses and receives activity notifications. |

## 4. Functional Requirements

### 4.1 Authentication & Account Management

| ID     | Requirement |
|--------|-------------|
| AUTH-1 | Teachers log in with username/password. |
| AUTH-2 | Passwords are hashed with bcrypt (cost ≥ 12). |
| AUTH-3 | Sessions use stateless JWT tokens (access + refresh). |
| AUTH-4 | Admin users can create, list, update, and delete teacher accounts via an admin API and/or CLI tool. |
| AUTH-5 | Students join sessions via a session code — no account required. |

### 4.2 Course Management

| ID      | Requirement |
|---------|-------------|
| COURSE-1 | Teachers can create, list, update, and archive courses. |
| COURSE-2 | Each course has a name, term/semester label, and owning teacher. |

### 4.3 Session Management

| ID     | Requirement |
|--------|-------------|
| SESS-1 | A teacher starts a session linked to a course. |
| SESS-2 | Starting a session generates a unique, short-lived join code. |
| SESS-3 | Students join by entering the join code on the web page. |
| SESS-4 | A teacher can have at most one active session at a time. |
| SESS-5 | The teacher can end a session, which locks further responses. |

### 4.4 Stoplight Comprehension Feedback

| ID      | Requirement |
|---------|-------------|
| COMP-1  | Students submit a stoplight value: GREEN (understand), YELLOW (partial), RED (struggling). |
| COMP-2  | Students can update their response at any time during an active session. |
| COMP-3  | The teacher dashboard shows a live aggregate (counts/percentages per color). |
| COMP-4  | Individual student responses are visible to the teacher. |

### 4.5 Activity Mode Broadcast

| ID     | Requirement |
|--------|-------------|
| ACT-1  | The teacher sets the current activity mode: GROUP, PARTNER, or SILENT. |
| ACT-2  | The mode change is pushed to all connected students in real time. |
| ACT-3  | The student UI prominently displays the current activity mode with the corresponding color. |

### 4.6 Statistics

| ID      | Requirement |
|---------|-------------|
| STAT-1  | Per-session stats: response distribution over time, final snapshot, student count. |
| STAT-2  | Per-course stats: aggregated across all sessions in a course. |
| STAT-3  | Lifetime stats: aggregated across all courses for a teacher. |
| STAT-4  | Stats are viewable by the owning teacher and by admins. |

### 4.7 Student Web Client

| ID     | Requirement |
|--------|-------------|
| WEB-1  | A server-rendered or SPA web page served by the application. |
| WEB-2  | Join screen: enter session code and display name. |
| WEB-3  | Session screen: three large stoplight buttons + current activity mode display. |
| WEB-4  | Responsive design — usable on phones, tablets, and desktops. |

### 4.8 Teacher Web Dashboard

| ID      | Requirement |
|---------|-------------|
| DASH-1  | Login page. |
| DASH-2  | Course list and management. |
| DASH-3  | Session start/stop controls. |
| DASH-4  | Live stoplight aggregate visualization. |
| DASH-5  | Activity mode selector. |
| DASH-6  | Session, course, and lifetime statistics views. |

## 5. Non-Functional Requirements

| ID     | Category      | Requirement |
|--------|---------------|-------------|
| NFR-1  | Security      | HTTPS in production. CSRF protection on state-changing endpoints. |
| NFR-2  | Security      | Input validation on all API endpoints. |
| NFR-3  | Security      | Role-based access control enforced at the API layer. |
| NFR-4  | Performance   | Real-time updates delivered within 1 second under normal load. |
| NFR-5  | Scalability   | Support ≥ 50 concurrent sessions, ≥ 200 students per session. |
| NFR-6  | Testability   | Unit tests for all service-layer logic. Integration tests for API endpoints. |
| NFR-7  | Maintainability | Follow SOLID principles and layered architecture. |
| NFR-8  | Deployment    | Containerized (Docker) for cloud deployment. |
| NFR-9  | Portability   | RESTful API enables future native clients (macOS, Android). |

## 6. Technology Stack

| Layer            | Choice | Rationale |
|------------------|--------|-----------|
| Language         | Java 21 (LTS) | Preferred language; strong OOP, mature ecosystem. |
| Framework        | Spring Boot 3.4 | Industry standard for Java REST APIs; built-in security, DI, testing. |
| Real-time        | WebSocket (STOMP over SockJS via Spring WebSocket) | Push activity modes to students; push aggregate updates to teachers. |
| Database         | PostgreSQL | Robust relational DB; good for structured stats and user data. |
| ORM              | Spring Data JPA (Hibernate) | Standard JPA with repository pattern. |
| Security         | Spring Security + JWT | Stateless auth for API; role-based access. |
| Build            | Gradle (Kotlin DSL) | Modern, flexible Java build tool. |
| Frontend         | Thymeleaf (server-rendered) + vanilla JS/CSS | Simple, no separate SPA build step; progressive enhancement. |
| Testing          | JUnit 5 + Mockito + Spring Boot Test | Standard Java testing stack. |
| Containerization | Docker + Docker Compose | Consistent dev/prod environments. |
| CI/CD            | GitHub Actions | Free for public repos; automates build, test, deploy. |

## 7. Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Clients                           │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │ Student  │  │ Teacher  │  │ Future Native App │ │
│  │ Web Page │  │Dashboard │  │ (macOS / Android) │ │
│  └────┬─────┘  └────┬─────┘  └────────┬──────────┘ │
│       │              │                 │            │
│       │   HTTP/REST + WebSocket        │            │
└───────┼──────────────┼─────────────────┼────────────┘
        │              │                 │
┌───────▼──────────────▼─────────────────▼────────────┐
│              Spring Boot Application                │
│                                                     │
│  ┌────────────┐  ┌────────────┐  ┌───────────────┐ │
│  │ Controller │  │  Service   │  │  Repository   │ │
│  │   Layer    │──▶   Layer    │──▶    Layer      │ │
│  │ (REST+WS)  │  │ (Business) │  │  (JPA/DB)    │ │
│  └────────────┘  └────────────┘  └───────────────┘ │
│                                                     │
│  ┌────────────┐  ┌────────────┐                     │
│  │  Security  │  │  WebSocket │                     │
│  │  Filters   │  │   Broker   │                     │
│  └────────────┘  └────────────┘                     │
└─────────────────────┬───────────────────────────────┘
                      │
               ┌──────▼──────┐
               │ PostgreSQL  │
               └─────────────┘
```

### 7.1 Package Structure

```
com.stoplight.classroom
├── config/          # Spring configuration, security, WebSocket config
├── controller/      # REST controllers and WebSocket message handlers
├── dto/             # Request/response data transfer objects
├── model/           # JPA entity classes
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic
├── security/        # JWT filter, auth provider, role enums
├── exception/       # Custom exceptions and global error handler
└── util/            # Helpers (code generator, etc.)
```

## 8. Data Model (Core Entities)

```
User (id, username, passwordHash, role[ADMIN|TEACHER], createdAt, updatedAt)
  │
  └──< Course (id, name, term, teacherId, archived, createdAt)
         │
         └──< Session (id, courseId, joinCode, activityMode, status[ACTIVE|ENDED], startedAt, endedAt)
                │
                ├──< StudentParticipant (id, sessionId, displayName, joinedAt)
                │       │
                │       └──< StoplightResponse (id, participantId, sessionId, value[GREEN|YELLOW|RED], submittedAt)
                │
                └──< SessionSnapshot (id, sessionId, greenCount, yellowCount, redCount, studentCount, capturedAt)
```

## 9. API Outline

### Auth
- `POST /api/auth/login` — returns JWT tokens
- `POST /api/auth/refresh` — refresh access token

### Admin
- `GET/POST/PUT/DELETE /api/admin/users` — CRUD teacher accounts

### Courses
- `GET/POST/PUT /api/courses` — teacher's courses
- `GET /api/courses/{id}/stats` — course-level stats

### Sessions
- `POST /api/sessions` — start session (returns join code)
- `PUT /api/sessions/{id}/end` — end session
- `PUT /api/sessions/{id}/activity-mode` — set activity mode
- `GET /api/sessions/{id}/stats` — session stats
- `POST /api/sessions/join` — student joins with code + display name

### Stoplight
- `POST /api/stoplight` — submit/update response (student)
- `GET /api/sessions/{id}/responses` — live responses (teacher, WebSocket preferred)

### WebSocket Topics
- `/topic/session/{id}/aggregate` — live stoplight counts → teacher
- `/topic/session/{id}/activity-mode` — activity mode changes → students

### Stats
- `GET /api/stats/lifetime` — teacher lifetime stats

## 10. Security Plan

1. All passwords hashed with bcrypt (cost ≥ 12).
2. JWT access tokens (short-lived, 15 min) + refresh tokens (longer-lived, 7 days).
3. Role-based method security (`@PreAuthorize`).
4. Student endpoints require a valid session participant token (lightweight, session-scoped).
5. Input validation via Bean Validation (`@Valid`).
6. Rate limiting on login endpoint.
7. CORS configured for allowed origins only.
8. SQL injection prevented by parameterized queries (JPA).
9. XSS mitigated by Thymeleaf auto-escaping and Content-Security-Policy headers.

## 11. Testing Strategy

| Level        | Scope | Tools |
|--------------|-------|-------|
| Unit         | Service classes, utilities, DTOs | JUnit 5 + Mockito |
| Integration  | Controllers, repositories, security filters | Spring Boot Test + Testcontainers (PostgreSQL) |
| WebSocket    | STOMP message flow | Spring WebSocket test support |
| End-to-end   | Full user flows (future) | Selenium or Playwright |

## 12. Deployment Plan

1. **Docker image**: Multi-stage Dockerfile (build with Gradle, run with JRE 21 slim).
2. **Docker Compose**: App + PostgreSQL for local development.
3. **Cloud target**: AWS (ECS/Fargate or EC2) or similar; PostgreSQL via RDS or managed DB.
4. **CI/CD**: GitHub Actions pipeline — build → test → Docker build → push to registry → deploy.
5. **Environment config**: Externalized via Spring profiles and environment variables.

## 13. Implementation Phases

### Phase 1 — Foundation
- [x] Project scaffolding (Spring Boot, Gradle, Docker Compose, PostgreSQL)
- [x] User entity, auth (login, JWT), admin CRUD
- [x] Unit + integration tests for auth

### Phase 2 — Core Session Flow
- [x] Course CRUD
- [x] Session start/end, join code generation
- [x] Student join flow
- [x] Stoplight response submission
- [x] WebSocket setup for real-time aggregates and activity mode

### Phase 3 — Web UI
- [x] Student join + session page (Thymeleaf + JS)
- [x] Teacher dashboard (login, courses, live session view)
- [x] Activity mode controls

### Phase 4 — Statistics
- [x] Session snapshots (periodic capture)
- [x] Per-session, per-course, lifetime stats endpoints
- [x] Stats views in teacher dashboard

### Phase 5 — Hardening & Deployment
- [x] Security audit (CORS, CSP, rate limiting, input validation)
- [x] Dockerfile + Docker Compose finalization
- [x] GitHub Actions CI/CD pipeline
- [x] Cloud deployment — AWS ECS Fargate + RDS PostgreSQL + ALB (Terraform in `infra/terraform/`)

### Phase 6 — Future
- [x] Native macOS client (Swift/SwiftUI consuming REST API) — scaffold with API client
- [x] Native Android client (Kotlin consuming REST API) — scaffold with API client
- [x] Student accounts (optional, for tracking across sessions)
- [x] Export stats (CSV)
