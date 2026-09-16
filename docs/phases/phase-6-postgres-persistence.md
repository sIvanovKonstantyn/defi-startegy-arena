# Phase 6 — Postgres + jOOQ + Flyway persistence

## Purpose

Persist strategies in **PostgreSQL** behind the existing `StrategyRepository` port using **Flyway** migrations and **jOOQ** codegen, while keeping the **in-memory** adapter for frozen context e2e. Ship **Docker Compose** for app + DB with pool and resource limits. Config defaults live in `app.properties` and are overridable by env vars.

## Acceptance criteria

- Full strategy **CRUD** (create / list / get / update / delete) works against Postgres via HTTP and `JooqStrategyRepository`.
- Context e2e continues to use `ApplicationComposition.createDefault()` → InMemory.
- `docker compose up` runs app + Postgres with documented mem/CPU/JVM/Hikari limits.
- Integration tests use **H2 with `MODE=PostgreSQL`** (see `H2PostgresModeSupport`) — **not Testcontainers** — so `./gradlew qualityCheck` stays hermetic; Compose still runs real Postgres for the app.

## Config

Defaults: [`src/main/resources/app.properties`](../../src/main/resources/app.properties).  
Override: env var = `DSA_` + property key with `.` → `_` and camelCase → `UPPER_SNAKE`.

| Property | Env | Default |
| --- | --- | --- |
| `persistence.mode` | `DSA_PERSISTENCE_MODE` | `memory` |
| `http.port` | `DSA_HTTP_PORT` | `8080` |
| `jdbc.driver` | `DSA_JDBC_DRIVER` | `org.postgresql.Driver` |
| `jdbc.url` | `DSA_JDBC_URL` | `jdbc:postgresql://localhost:5432/dsa` |
| `jdbc.user` | `DSA_JDBC_USER` | `dsa` |
| `jdbc.password` | `DSA_JDBC_PASSWORD` | `dsa` |
| `hikari.maximumPoolSize` | `DSA_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `hikari.minimumIdle` | `DSA_HIKARI_MINIMUM_IDLE` | `1` |
| `hikari.connectionTimeoutMs` | `DSA_HIKARI_CONNECTION_TIMEOUT_MS` | `5000` |
| `hikari.idleTimeoutMs` | `DSA_HIKARI_IDLE_TIMEOUT_MS` | `600000` |
| `hikari.maxLifetimeMs` | `DSA_HIKARI_MAX_LIFETIME_MS` | `1800000` |
| `hikari.poolName` | `DSA_HIKARI_POOL_NAME` | `dsa-strategy` |

Compose sets `DSA_PERSISTENCE_MODE=postgres` and JDBC/Hikari env only (no second properties file).

## Schema

Flyway `V1__strategies.sql`: current version only; unique `(owner_id_normalized, name_normalized)`; `definition_json` as `VARCHAR` holding the JSON document (HTTP rule wire fields) — portable for H2 PostgreSQL-mode ITs and real Postgres.

## Local run

```bash
docker compose up --build
# API: http://localhost:8080
```

## Out of scope

- Transactional outbox for domain events
- Soft-delete / version history API
- Marketdata / arena / leaderboard tables
