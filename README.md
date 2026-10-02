# FairShare backend (Phase 1)

Lean Spring Boot backend for registration, login, JWT authentication, and the current user endpoint. Expenses, balances, settlements, groups, dashboard, and AI are intentionally out of scope.

## Configuration

Copy `.env.example` to `.env` and set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 bytes), `JWT_EXPIRATION_MS`, and optionally `PORT`. Spring Boot reads these variables from the process environment. Defaults target a local PostgreSQL database named `fairshare`. Flyway applies the initial schema migration automatically.

Start PostgreSQL for local development with:

```bash
docker compose up -d postgres
```

The frontend uses `VITE_API_URL` at build time and defaults to `http://localhost:8080/api`.

## Run and validate

```bash
mvn test
mvn spring-boot:run
```

Endpoints: `POST /api/auth/register`, `POST /api/auth/login`, and authenticated `GET /api/auth/me`. Send JWT responses as `Authorization: Bearer <token>`.
