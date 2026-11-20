# TaskFlow Demo Backend

Spring Boot 3 / Java 21 REST API for the TaskFlow demo task-management app. This is the
Application Under Test (AUT) backend for the TaskFlow QA engineering project - see
`../API_CONTRACT.md` for the full API contract.

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Docker + Docker Compose (only needed for the `dev` profile against real MySQL)

## Running MySQL locally (for the `dev` profile)

From the `aut-demo/` directory (one level up from this backend):

```bash
cp .env.example .env     # fill in real values; .env is gitignored
docker compose up -d
```

This starts a `mysql:8` container on `localhost:3306` with a named volume and healthcheck.

## Configuring the backend

```bash
cd backend
cp .env.example .env     # fill in real values; .env is gitignored
```

The backend reads its MySQL connection and JWT secret from environment variables
(`JWT_SECRET`, `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`), with
localhost-friendly defaults for local development. `application.yml`'s `dev`/`docker` profiles
include an insecure placeholder `JWT_SECRET` default so the app can start without any setup -
this placeholder must never be relied on outside a local machine; CI and any shared/deployed
environment must set a real `JWT_SECRET` via the environment.

## Running the app

### Dev mode (real MySQL via docker-compose)

```bash
mvn spring-boot:run
```

Uses the `dev` profile by default, connecting to MySQL at `localhost:3306` (or wherever
`DB_HOST`/`DB_PORT` point).

### Tests (H2, zero external dependencies)

```bash
mvn test
```

Runs against the `test` Spring profile, which uses an in-memory H2 database in MySQL-compatibility
mode (`DB_CLOSE_DELAY=-1;MODE=MySQL`). No MySQL server or `.env` file is required.

## Endpoints

All endpoints are prefixed with `/api`. See `../API_CONTRACT.md` for full request/response
shapes and status codes.

| Method | Path                | Description                                               |
|--------|---------------------|-------------------------------------------------------------|
| POST   | `/auth/register`    | Create a new user account                                 |
| POST   | `/auth/login`        | Authenticate and receive a JWT bearer token                |
| GET    | `/users/me`           | Get the authenticated user's profile                       |
| GET    | `/tasks`              | List the authenticated user's tasks (filter/search/sort)   |
| POST   | `/tasks`              | Create a task owned by the authenticated user               |
| GET    | `/tasks/{id}`         | Get a single task owned by the authenticated user           |
| PUT    | `/tasks/{id}`         | Update a task owned by the authenticated user                |
| DELETE | `/tasks/{id}`         | Delete a task owned by the authenticated user                |
| GET    | `/dashboard`          | Get task counts (total/todo/in-progress/done) for the user  |

All endpoints except `/auth/register` and `/auth/login` require an
`Authorization: Bearer <token>` header.
