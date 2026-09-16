# Phase 8 — Strategy session binding via outbox/inbox

**Status:** implemented  
**Contexts:** `bootstrap`, `identity` (session resolve), `strategy`, `shared.messaging` / `shared.infra.messaging`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md)

---

## Purpose

Stop accepting client-supplied `ownerId` on strategy HTTP. Authenticate with Bearer token at the API edge, enqueue correlated strategy **request** events through a **Postgres transactional outbox**, have strategy **listeners** process them (inbox for idempotent consume), and enqueue **response** events the same way. HTTP returns **`202 Accepted` + `correlationId`** immediately; mapping response events back to HTTP is deferred.

---

## Decisions

| Topic | Choice |
| --- | --- |
| Auth | Bootstrap calls `GetCurrentUser` (sync identity at edge only) |
| Cross-context | Request/response via `DomainEvent` + outbox/inbox; no identity↔strategy imports |
| Delivery | `messaging_outbox` / `messaging_inbox` tables; in-process `OutboxRelay` |
| HTTP success | `202` + `{ "correlationId": "…" }` |
| HTTP auth fail | `401` (no outbox write) |
| Memory mode | In-memory outbox/inbox stores with same ports |

---

## In scope

- Strip `ownerId` from strategy HTTP inputs
- Outbox publisher + relay + strategy request listeners
- Response events (`*Completed` / `*Failed`) written to outbox
- Flyway `V3__messaging_outbox.sql`

## Out of scope

- Response outbox → HTTP response bridge
- External broker (Kafka, etc.)
- Load-test script updates for 202 contract

---

## E2E freeze (sketch)

1. Without Bearer → strategy routes `401`
2. With valid session → `202` + non-blank `correlationId`; create without `ownerId` in body
3. Relay processes request; response event for same correlation appears in outbox

---

## Flow

See [strategy-authenticated-async.md](../flows/strategy-authenticated-async.md).
