# Phase 4 — List and get strategies

**Status:** implemented  
**Depends on:** phases 1–3  
**Context:** `strategy`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md)

Frozen e2e: `src/test/java/com/defistrategyarena/strategy/e2e/StrategyReadsE2ETest.java`  
Jetty e2e: `src/test/java/com/defistrategyarena/bootstrap/StrategyReadsHttpE2ETest.java`  
Flow: [`docs/flows/strategy-reads.md`](../flows/strategy-reads.md)

---

## Purpose

Let an owner **list** their strategies (paginated + sorted) and **get** one strategy’s full definition, scoped by opaque `ownerId` (no auth yet). Private strategies of other owners are not readable (`404`).

---

## Routes

| Method | Path | Query |
| --- | --- | --- |
| `GET` | `/strategies` | `ownerId` (required), `page`, `size`, `sort`, `order` |
| `GET` | `/strategies/{strategyId}` | `ownerId` (required) |

### List pagination / sort

| Query | Default | Rules |
| --- | --- | --- |
| `page` | `0` | `>= 0` |
| `size` | `20` | `1..100` |
| `sort` | `name` | `name` \| `strategyId` |
| `order` | `asc` | `asc` \| `desc` |

Invalid paging/sort → `400`. Unknown owner → `200` empty page.

### Responses

- List item: `strategyId`, `name`, `privacy`, `versionNumber`
- Detail: list fields + `rules` (create wire shape)
- Get wrong owner / missing id → `404`
- Blank `ownerId` → `400`

---

## E2E scenarios

### L1 — List page sorted by name
### L2 — Second page / totals
### L3 — Empty owner list
### L4 — Invalid page/size/sort/order → 400
### G1 — Get detail with rules
### G2 — Get wrong owner / unknown id → 404
### G3 — Blank owner → 400

---

## Out of scope

Authn, multi-field sort, cursor pagination, update/delete, share.
