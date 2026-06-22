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

The application connects to PostgreSQL on `localhost:5432` (database `creatorhub`,
user `creatorhub`, password `creatorhub` — development credentials only).

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

See [CLAUDE.md](CLAUDE.md) for the full project guide, working rules and phase progress.
