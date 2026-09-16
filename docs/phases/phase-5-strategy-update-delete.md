# Phase 5 — Strategy update and delete

**Status:** implemented  
**Depends on:** phases 1–4  
**Context:** `strategy`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md)

Frozen e2e: `src/test/java/com/defistrategyarena/strategy/e2e/StrategyUpdateDeleteE2ETest.java`  
Jetty e2e: `src/test/java/com/defistrategyarena/bootstrap/StrategyUpdateDeleteHttpE2ETest.java`  
Flow: [`docs/flows/strategy-update-delete.md`](../flows/strategy-update-delete.md)

---

## Purpose

Let an owner **update** a strategy’s rules as a new version (name immutable — it anchors the idempotent id) and **hard-delete** a strategy they own.

---

## Routes

| Method | Path | Query | Body |
| --- | --- | --- | --- |
| `PUT` | `/strategies/{strategyId}` | `ownerId` (required) | `{ "rules": [...] }` |
| `DELETE` | `/strategies/{strategyId}` | `ownerId` (required) | (none) |

### Update

- Replaces the entire `rules` list; reuses current **name**
- Bumps `versionNumber` by 1; `strategyId` and `privacy` unchanged
- Success: `200` + `{ status, strategyId, versionNumber }`
- Publishes `StrategyVersionPublished` for the new version
- Empty / null / invalid rules → `400`
- Wrong owner / unknown id → `404`

### Delete

- Hard remove from store and owner-name index
- Success: `200` + `{ status, strategyId }`
- Wrong owner / unknown id → `404`
- Blank `ownerId` → `400`
- No domain event in this slice

---

## E2E scenarios

### U1 — Update bumps version, keeps name, persists rules, emits event
### U2 — Update wrong owner / unknown id → 404
### U3 — Invalid / empty rules → 400
### D1 — Delete removes strategy (get → 404)
### D2 — Delete wrong owner → 404
### D3 — Blank owner → 400

---

## Out of scope

Authn, rename, soft-delete, version history API, share/privacy changes, PATCH, arena/marketdata.
