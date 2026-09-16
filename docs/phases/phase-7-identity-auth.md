# Phase 7 — Identity sign-up / sign-in

**Status:** implemented  
**Context:** `identity`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md) — scenarios → freeze e2e → implement

Frozen e2e: `src/test/java/com/defistrategyarena/identity/e2e/IdentityAuthE2ETest.java`

---

## Purpose

Let users **register and authenticate** so the platform has a real subject for ownership. Support **email + password** (password stored only as a **one-way hash**, PBKDF2-HMAC-SHA256).

Issue an opaque **Bearer session token** for subsequent API calls. Signup/login return only `accessToken` (no `userId`). Strategy routes still accept opaque `ownerId` in this phase; a later phase will resolve the owner from the Bearer token server-side.

Cloud identity providers (e.g. Google OAuth) are intentionally out of scope for now.

---

## Decisions

| Topic | Choice |
| --- | --- |
| Password storage | PBKDF2-HMAC-SHA256 + random salt (JDK); never store plaintext |
| Session | Opaque token; only SHA-256 hash stored; client sends `Authorization: Bearer …` |
| Cross-context | Identity does not call strategy; composition root wires routes; no sync context imports |
| Persistence | Flyway `V2__identity.sql` + in-memory adapters for e2e / `createDefault` |

---

## In scope

| Item | Detail |
| --- | --- |
| `POST /auth/signup` | email, password, displayName → 201 + session |
| `POST /auth/login` | email, password → 200 + session |
| `POST /auth/logout` | Bearer → 204 |
| `GET /auth/me` | Bearer → 200 `{ "displayName": "…" }` |
| Config | `auth.sessionTtlSeconds` via `app.properties` + `DSA_*` |

## Out of scope

- Google / other cloud OAuth providers
- Refresh tokens / rotating sessions
- Email verification / password reset
- Binding strategy handlers to session (still query/body `ownerId`)
- Roles / RBAC beyond “authenticated user”
- HttpOnly cookie delivery (Bearer JSON is enough for API/k6)

---

## E2E scenarios (freeze list)

Entry: **`IdentityRestAdapter`** with in-memory repos.

### S1 — Sign up with email/password

**Given** unused email + valid password  
**When** `signup`  
**Then** `201`, non-blank `accessToken` (no `userId`); `me(token)` returns same `displayName`; password is not echoed

### S2 — Reject duplicate email

**Given** existing user  
**When** signup with same email  
**Then** `409`; no second user

### S3 — Login success / failure

**Given** registered user  
**When** correct password → `200` + token; wrong password → `401`

### S4 — Logout invalidates session

**Given** valid token  
**When** logout then `me`  
**Then** logout `204`; `me` → `401`

---

## HTTP contract (sketch)

```http
POST /auth/signup
{"email":"a@b.co","password":"secret-value","displayName":"Ada"}

POST /auth/login
{"email":"a@b.co","password":"secret-value"}

Authorization: Bearer <accessToken>
GET /auth/me
POST /auth/logout
```

## Persistence note

Phase 7 ships Flyway `V2__identity.sql` for Postgres schema. Runtime composition uses **in-memory** identity repositories for both `memory` and `postgres` modes until jOOQ identity adapters are added.
