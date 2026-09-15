# Phase 1 — Create private strategy (approval package)

**Status:** approved — implementing / implemented  
**Context:** `strategy`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md) — scenarios → approval → freeze e2e → implement

Frozen e2e: `src/test/java/com/defistrategyarena/strategy/e2e/CreateStrategyE2ETest.java`

Illustrative shapes already exist in [`docs/examples/`](../examples/). This document is the **product contract** for the first implementation slice.

---

## Purpose

Let an owner create a **private** strategy definition through the strategy context’s HTTP-shaped boundary (`adapter.web`), persist it under a **unique (ownerId, name)** constraint, and notify other contexts asynchronously that a version exists — without arena submission, sharing, or export yet.

---

## In scope

| Item | Detail |
| --- | --- |
| Create strategy | Via `StrategyRestAdapter.create(CreateStrategyHttpRequest)` |
| Privacy default | Always `PRIVATE` on create |
| Strategy id | Derived via `IdempotentUuid` from create fields (stable when inserted once) |
| Uniqueness | Persistence enforces **unique index on `(ownerId, name)`**; duplicate create is **rejected** |
| Minimal DSL | Rules with `price_above` condition + `hold` action (v1 starter set) |
| Persistence | Outbound `StrategyRepository` port; e2e uses in-memory adapter that enforces the unique key |
| Integration event | Publish `StrategyVersionPublished` only on **successful** create |
| Context e2e | Enter **only** through `adapter.web` (not application use case directly) |

## Out of scope (later phases)

- Share / ACL grants / public definition
- Export formats
- Publish additional versions as a separate explicit “publish” product action (create stores initial version `1`)
- Arena submission / backtest / leaderboard
- Identity authn (owner is an opaque string for now)
- Jetty `POST /strategies` route wiring (optional follow-up once adapter e2e is green; composition root only)
- React studio

---

## Actors & packages

| Actor | Package |
| --- | --- |
| E2E / future HTTP stack | `strategy.adapter.web` |
| Use case | `strategy.application` |
| Aggregate + DSL | `strategy.domain` |
| In-memory repo (test/dev) | `strategy.adapter.persistence` |
| Event type | `shared.events.strategy` |
| Event bus port | `shared.messaging` |

Flow sequence: [`docs/flows/strategy-create.md`](../flows/strategy-create.md).

---

## E2E scenarios (proposed freeze list)

Entry point for every scenario: **`StrategyRestAdapter`** with in-memory repository + recording `DomainEventPublisher`.

**Persistence rule (all scenarios):** e2e must assert store state via the repository — success paths prove the strategy **was persisted** (`get(id)` is present and/or by owner+name); failure paths prove **no new row** was written (empty store or unchanged count / same aggregate). Repository lookups return `Optional` (never null).

### S1 — Create private strategy, persist it, and emit version published

**Given** a valid create request (owner, name, one `price_above` / `hold` rule)  
**When** `http.create(request)`  
**Then**

1. Response status is `201`
2. Response `strategyId` is non-blank
3. **Persisted:** `strategies.get(strategyId)` is non-null
4. **Persisted:** loaded aggregate has the same `ownerId` and definition `name` as the request
5. **Persisted:** privacy is `PRIVATE` and current version number is `1`
6. Exactly one `StrategyVersionPublished` was published with matching `strategyId`, `ownerId`, and version `1`

**Proposed test:** `creates_private_strategy_via_rest_adapter_and_emits_version_published`

### S2 — Reject duplicate owner and name

**Given** a strategy was already created for owner `O` and name `N`  
**When** create is called again with the same `ownerId` and `name` (rules may differ)  
**Then**

1. First response status is `201` and the strategy **is persisted** under the returned id
2. Second response status is `409` (conflict)
3. **Persisted:** repository still contains **exactly one** strategy for `(O, N)` (same id as the first response)
4. No additional `StrategyVersionPublished` was published for the rejected attempt

**Proposed test:** `rejects_duplicate_owner_and_name`

Persistence contract: unique index / key on `(ownerId, name)`. The in-memory adapter must enforce the same rule as a future DB unique constraint.

### S3 — Reject blank owner

**Given** `ownerId` blank or null  
**When** create is called  
**Then**

1. Response is not a successful create (`201` must not occur)
2. **Not persisted:** repository remains empty (count `0` / no strategies stored)
3. No `StrategyVersionPublished` was published

**Proposed test:** `rejects_blank_owner`

### S4 — Reject blank strategy name

**Given** blank or null `name`  
**When** create is called  
**Then** same as S3: not `201`, **repository empty**, no event

**Proposed test:** `rejects_blank_strategy_name`

### S5 — Reject unknown rule type

**Given** a rule whose `type` is not in the v1 allow-list (`price_above` only for phase 1)  
**When** create is called  
**Then**

1. Response is not a successful create
2. **Not persisted:** repository remains empty
3. No `StrategyVersionPublished` was published

**Proposed test:** `rejects_unknown_rule_type`

---

## HTTP-shaped contract (adapter boundary)

```text
CreateStrategyHttpRequest
  ownerId: String
  name: String
  rules: List<RuleBody>
    RuleBody
      id: String
      type: String          # phase 1: "price_above"
      instrument: String
      threshold: String

CreateStrategyHttpResponse
  status: int               # 201 created | 409 duplicate owner+name
  strategyId: String        # non-blank on 201; empty/absent semantics on 409 — use empty string in phase 1
```

Integration event:

```text
StrategyVersionPublished(strategyId, versionNumber, ownerId) implements DomainEvent
```

---

## Decisions locked for phase 1 (please confirm)

| # | Decision | Proposal |
| --- | --- | --- |
| D1 | Emit `StrategyVersionPublished` on **create** (not only on a later publish action) | **Yes** |
| D2 | `ownerId` remains a plain `String` until Identity context exists | **Yes** |
| D3 | Same owner + name on a second create | **Reject** (`409`); unique persistence key `(ownerId, name)` — not upsert / not silent idempotent success |
| D4 | Empty `rules` list allowed on create | **Yes** (S1 still uses one rule) |
| D5 | Context e2e does **not** require Jetty; Jetty route is a later wiring step | **Yes** |

---

## Approval checklist

- [x] Scope in / out of phase 1
- [x] Scenarios S1–S5
- [x] Decisions D1–D5 (including unique `(ownerId, name)` reject)
- [x] HTTP DTO field names

Frozen tests: `src/test/java/com/defistrategyarena/strategy/e2e/CreateStrategyE2ETest.java`  
Implementation is under `strategy.domain` / `application` / `adapter.*` and `shared.events.strategy`.
