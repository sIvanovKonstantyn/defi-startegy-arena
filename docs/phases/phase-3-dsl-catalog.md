# Phase 3 — V1 DSL catalog

**Status:** implemented  
**Depends on:** [phase-1-strategy-create.md](./phase-1-strategy-create.md), [phase-2-jetty-create-strategy.md](./phase-2-jetty-create-strategy.md)  
**Context:** `strategy`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md) — scenarios → freeze e2e → implement

Frozen e2e: `src/test/java/com/defistrategyarena/strategy/e2e/CreateStrategyE2ETest.java`

Flow: [`docs/flows/strategy-dsl-catalog.md`](../flows/strategy-dsl-catalog.md)

---

## Purpose

Lock the remaining v1 strategy DSL **condition** and **action** allow-list (beyond phase-1 `price_above` / `hold`), expose them on the HTTP create contract with explicit `conditionType` / `actionType`, and reject unknown types or invalid buy/sell allocations without persisting.

---

## Catalog lock

| Kind | Wire | Domain |
| --- | --- | --- |
| Conditions | `price_above`, `price_under`, `indicator_below`, `indicator_above` | `PriceAbove`, `PriceUnder`, `IndicatorBelow`, `IndicatorAbove` |
| Actions | `hold`, `buy`, `sell` | `Hold`, `Buy`, `Sell` |

## In scope

| Item | Detail |
| --- | --- |
| Domain sealed permits | Expand `Condition` / `Action` as above |
| HTTP wire | `RuleBody` with `conditionType`, `actionType`, `instrument`, `indicator`, `threshold`, `allocationPercent` |
| Mapping | Adapter maps allow-listed types; unknown → `400` |
| Buy / sell | Require non-blank `instrument` + `allocationPercent` |
| E2E | Context adapter scenarios below; Jetty bodies updated to new JSON |

## Out of scope

- and/or/not condition trees
- Further indicators / protocol actions
- Marketdata snapshots, interpreter, arena, leaderboard
- Dual support for phase-1 `"type": "price_above"` field (intentional contract bump)

---

## HTTP-shaped contract

```text
CreateStrategyHttpRequest
  ownerId, name
  rules: List<RuleBody>
    RuleBody
      id: String
      conditionType: String   # price_above | price_under | indicator_below | indicator_above
      actionType: String      # hold | buy | sell
      instrument: String      # price_* conditions and buy/sell target
      indicator: String       # indicator_* conditions (blank for price_*)
      threshold: String
      allocationPercent: String  # required for buy/sell; blank for hold
```

---

## E2E scenarios

Entry: `StrategyRestAdapter` + in-memory repo + recording publisher. Persistence asserted on every scenario.

### D1 — Create with `price_above` + `hold`

**Given** valid request with one `price_above` / `hold` rule  
**When** create  
**Then** `201`, persisted, one `StrategyVersionPublished`

### D2 — Create with `price_under` + `sell`

**Given** `price_under` + `sell` with non-blank `instrument` and `allocationPercent`  
**When** create  
**Then** `201`, persisted rule uses `PriceUnder` + `Sell`

### D3 — Create with `indicator_below` + `hold` and `indicator_above` + `buy`

**Given** rules using indicator conditions (and buy with allocation + trade `instrument`)  
**When** create  
**Then** `201`, persisted

### D4 — Reject unknown condition type

**Given** `conditionType` not in allow-list  
**When** create  
**Then** `400`, store empty, no event

### D5 — Reject unknown action type

**Given** `actionType` not in allow-list  
**When** create  
**Then** `400`, store empty, no event

### D6 — Reject blank allocation on buy/sell

**Given** `buy` or `sell` with blank `allocationPercent`  
**When** create  
**Then** `400`, store empty, no event

Phase-1 scenarios (duplicate name, blank owner/name) remain; bodies use the new `RuleBody` shape.

---

## Decisions

| # | Decision | Lock |
| --- | --- | --- |
| D1 | Split wire `type` into `conditionType` + `actionType` | Yes |
| D2 | Indicator id on wire is `indicator`; price/trade target is `instrument` | Yes |
| D3 | No backward-compatible phase-1 `type` field | Yes |

## Approval checklist

- [x] Catalog table
- [x] Scenarios D1–D6
- [x] Wire field names
