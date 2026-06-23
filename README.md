# CreatorHub

A creator-subscriptions platform — a skeleton in the spirit of Patreon / Fanvue.
Creators publish free and premium content; fans subscribe to paid tiers, access
premium content, comment, and receive notifications.

> University project (Aplicații Web cu Arhitectură de Microservicii), built to
> production quality. Strategy: **monolith first, microservices last.** The
> current codebase is the Spring Boot monolith skeleton; Spring Cloud, caching,
> MongoDB, monitoring and the React frontend come in later phases.

## Tech stack (current)

- Java 21
- Spring Boot 3.5.x (Web, Data JPA, Validation, Lombok, DevTools)
- Maven (via the Maven Wrapper — no local Maven install required)
- PostgreSQL 16 (Docker)

## Getting started

Prerequisites: Java 21 and Docker.

```bash
# 1. Start the database
docker compose up -d

# 2. Compile the project
./mvnw clean compile        # Windows: mvnw.cmd clean compile

# 3. (later) Run the application
./mvnw spring-boot:run
```

The application connects to PostgreSQL on `localhost:5433` (host port mapped to
the container's 5432; database `creatorhub`, user `creatorhub`, password
`creatorhub` — development credentials only) and serves HTTP on port `8081`.
Both ports avoid common dev-machine conflicts (a native PostgreSQL on 5432, an
Oracle listener on 8080).

## Frontend (React SPA)

A separate React + TypeScript + Vite app lives in [`frontend/`](frontend). It is
a night-owl cyberpunk UI (dark + electric violet, glow, motion) that consumes the
REST API. Prerequisite: Node.js 18+.

```bash
# run the full stack
docker compose up -d          # database
./mvnw spring-boot:run        # backend on :8081 (dev profile)

cd frontend
npm install
npm run dev                   # frontend on http://localhost:5173
```

Open **http://localhost:5173**. The Vite dev server proxies `/api` to the backend
on `:8081`, which keeps the SPA same-origin with the API so the cookie-based CSRF
flow works (see [CLAUDE.md](CLAUDE.md) §15). Log in on dev with `admin` / `admin123`,
or register a new account. All UI text is in English.

Stack: React Router, TanStack Query, Framer Motion (animations), React Hook Form
(validation). Pages: feed (with premium gating), post detail + comments, creators,
tiers, subscriptions, create/edit post, profile, admin, and a custom 404.

> Screenshots: add under `docs/screenshots/` (feed, premium lock overlay, login).

## Environments (Spring profiles)

Two profiles, each backed by its own database:

| Profile | Database | When |
|---------|----------|------|
| `dev` (default) | PostgreSQL 16 in Docker (`localhost:5433`) | running the app locally |
| `test` | H2 in-memory | automated tests (`@ActiveProfiles("test")`) |

```bash
# DEV — needs Docker (the default profile)
docker compose up -d
./mvnw spring-boot:run          # active profile: dev, HTTP on :8081

# TEST — needs NO Docker (H2 in-memory)
./mvnw clean test               # active profile: test
```

Config files: `application.yml` (common, defaults the active profile to `dev`),
`application-dev.yml` (PostgreSQL), `application-test.yml` (H2). A production
profile with externalized secrets will be added in a later phase.

## Security & authentication

Session-based authentication backed by the database (BCrypt passwords, CSRF
active, roles `USER` / `ADMIN`, token-based remember-me). It is exposed via REST
so a React SPA can consume it; a minimal custom login page is served at
[`/login`](http://localhost:8081/login) for manual testing.

Endpoints (run on dev, `:8081`):

| Method & path | Access | Purpose |
|---|---|---|
| `POST /api/auth/register` | public | create a `USER` account |
| `POST /api/auth/login` | public | log in (creates a session) |
| `POST /api/auth/logout` | session | log out |
| `GET /api/auth/me` | authenticated | current user |
| `GET /api/admin/users` | `ADMIN` | list all users |

**CSRF:** state-changing requests need the CSRF token. Clients first do a `GET`
(e.g. load `/login`) to receive the `XSRF-TOKEN` cookie, then send it back in the
`X-XSRF-TOKEN` header. The static login page does this automatically.

**Dev admin (development only):** on the `dev` profile an admin is seeded at
startup — username `admin`, password `admin123`. These are development-only
credentials and are never seeded in `test` or production. Self-registration only
ever creates `USER` accounts.

See [CLAUDE.md](CLAUDE.md) §11 for the full security design (login-page decision,
CSRF for SPA, remember-me, URL authorization convention, and how JWT will be added
on top at the microservices stage).

## REST API & docs

The backend is a REST API under `/api`. Interactive documentation is generated
with **springdoc / Swagger UI**:

- Swagger UI: [`/swagger-ui.html`](http://localhost:8081/swagger-ui.html)
- OpenAPI spec: [`/v3/api-docs`](http://localhost:8081/v3/api-docs)

Highlights (full table in [CLAUDE.md](CLAUDE.md) §14):

- Resources: `posts`, `creators`, `profiles`, `tiers`, `subscriptions`, `comments`,
  `tags`, plus `auth` and `admin`. Standard verbs (GET/POST/PUT/DELETE) and codes
  (200/201/204/400/401/403/404/409).
- **Owner from context:** create requests do NOT take an owner id — the owner is
  the authenticated user. Update/delete require the owner (or an admin).
- **Premium gating:** premium posts come back `locked: true` with no `body` unless
  you’re the author, an admin, or an active subscriber. Same rule gates commenting
  on premium posts.
- **Validation:** invalid request bodies return `400` with a `fieldErrors` map
  (field → message). Malformed JSON also returns `400`.

```jsonc
// 400 example
{ "status": 400, "error": "Bad Request", "message": "Validation failed",
  "fieldErrors": { "name": "Tier name is required",
                   "priceMonthly": "Monthly price must be greater than 0" } }
```

### CORS (for the React SPA in 7B)

CORS allows the dev front-end origins `http://localhost:5173` / `:3000` with
credentials. Because a cross-origin SPA cannot read the `XSRF-TOKEN` cookie, the
**recommended dev setup is a Vite proxy** (`/api` → `:8081`, same-origin), or the
SPA can read the token from `GET /api/auth/csrf` and send it in `X-XSRF-TOKEN`.
Custom server error pages live at `static/error/404.html` and `500.html`.

## Pagination & sorting

`Post`, `User` and `Subscription` support pagination and sorting. The service
layer returns a stable `PagedResponse<T>` and, once REST controllers exist (Views
phase), the API will accept the standard Spring Data params:

```
GET /api/posts?page=0&size=20&sort=title,asc
GET /api/posts?page=1&size=10&sort=createdAt,desc
```

- Page numbers are **0-based**; default size **20**, hard max **100**.
- Allowed sort fields (whitelisted — anything else returns `400`):
  posts `id,title,createdAt,premium`; users `id,username,email,role,enabled`
  (never `password`); subscriptions `id,startDate,status`.

## Logging

SLF4J + Logback (`logback-spring.xml`). Logs go to the console and to files under
`logs/` (git-ignored):

- `logs/creatorhub.log` — general log (rolling by size/day).
- `logs/creatorhub-error.log` — **errors only** (`ERROR` and above).

Levels per profile: `dev` logs `com.creatorhub` at `DEBUG`; `test` is quiet
(`WARN`, console only). An AOP aspect logs service method entry/exit and timing at
`DEBUG`. Passwords and tokens are **never** logged.

## Testing

89 tests, in two clearly separated kinds (all run with `./mvnw test`, no Docker):

- **Unit tests** (`*ServiceImplTest`) — JUnit 5 + Mockito, no Spring context and
  **no database** (repositories are mocked). One per service implementation.
- **Integration tests** (`*IntegrationTest`) — `@SpringBootTest` on the `test`
  profile against **H2 in-memory**. Three end-to-end scenarios: the business flow
  (creator → tier → premium post → subscribe → comment), security/authorization,
  and pagination/sorting.

```bash
./mvnw clean test       # run all tests + generate the coverage report
./mvnw clean verify     # also enforce the 70% service-layer coverage gate
```

Coverage is measured with **JaCoCo**. The HTML report is at
`target/site/jacoco/index.html`. The service layer (`com.creatorhub.service.impl`)
is at **~89% line coverage**, and `mvn verify` fails the build if it drops below
70%. Non-logic classes (entities, DTOs/mappers, config, security wiring, the AOP
aspect, the main class, simple exceptions) are excluded so the number reflects
real business logic.

## Project layout

```
src/main/java/com/creatorhub
├── controller   # REST endpoints
├── service      # business logic
├── repository   # Spring Data JPA repositories
├── model        # JPA entities
├── dto          # request/response data transfer objects
├── exception    # custom exceptions + global handler
└── config       # Spring configuration
```

## Data model (domain)

The domain has **7 entities** plus one many-to-many join table (`post_tags`).
Enums (`Role`, `SubStatus`) live in `com.creatorhub.model.enums` and are stored
as strings.

### Entities

- **User** — the base account. The same account can act as both a creator and a
  fan. Fields: `username` (unique), `email` (unique), `password` (will hold a
  BCrypt hash from the Security phase), `role` (`USER` / `ADMIN`), `enabled`.
  Mapped to the table `users` (`user` is a reserved word in PostgreSQL).
- **Profile** — public profile data, kept separate from the account: `displayName`,
  `bio` (up to ~1000 chars), `avatarUrl` (optional).
- **SubscriptionTier** — a paid level offered by a creator (e.g. "Basic", "VIP"):
  `name`, `priceMonthly` (`BigDecimal`, `numeric(10,2)`), `perks` (optional).
- **Post** — a piece of content, free or premium: `title`, `body` (TEXT),
  `premium` flag, `createdAt` (set on insert).
- **Subscription** — a fan's subscription to a tier; a link entity with its own
  data: `startDate` (set on insert), `status` (`ACTIVE` / `CANCELLED` / `EXPIRED`).
- **Comment** — a comment on a post: `text`, `createdAt` (set on insert).
- **Tag** — a label for posts: `name` (unique).

### Relationships

- **User 1—1 Profile.** A user has exactly one profile. *Profile* is the owning
  side and holds the `user_id` FK (so `user_id` lives in the `profile` table).
  Deleting a user deletes its profile (cascade + orphan removal).
- **User 1—N SubscriptionTier.** As a creator, a user offers many tiers
  (`subscription_tier.creator_id`).
- **User 1—N Post.** As a creator, a user authors many posts (`post.author_id`).
- **User 1—N Comment.** A user writes many comments (`comment.author_id`).
- **User 1—N Subscription.** As a fan, a user holds many subscriptions
  (`subscription.fan_id`).
- **SubscriptionTier 1—N Subscription.** A tier has many subscriptions
  (`subscription.tier_id`).
- **SubscriptionTier 1—N Post.** A tier gates many posts (`post.tier_id`,
  nullable — a free post has no tier).
- **Post 1—N Comment.** A post has many comments (`comment.post_id`). Deleting a
  post deletes its comments (cascade + orphan removal).
- **Post N—N Tag.** Posts and tags relate many-to-many through the `post_tags`
  join table. *Post* is the owning side (declares `@JoinTable`); *Tag* is the
  inverse side (`mappedBy`).

All `@ManyToOne`, `@OneToMany` and `@ManyToMany` associations are fetched
**LAZY**. A diagram image will be added later; this is the textual ER reference.

---

See [CLAUDE.md](CLAUDE.md) for the full project guide, working rules and phase progress.
