# Phase 10 — Strategy model extension

**Status:** in progress  
**Depends on:** [phase-9-web-ui-websocket.md](./phase-9-web-ui-websocket.md), [phase-6-postgres-persistence.md](./phase-6-postgres-persistence.md), [phase-3-dsl-catalog.md](./phase-3-dsl-catalog.md)  
**Context:** `strategy`, `shared.indicators`  
**Delivery rule:** [system-architecture.md §9](../system-architecture.md) — scenarios → freeze e2e → implement

Frozen e2e: `src/test/java/com/defistrategyarena/strategy/e2e/CreateStrategyE2ETest.java` (extended), repository IT for trees/FKs, UI Playwright create with AND tree.

Flows:

- [`docs/flows/strategy-condition-trees.md`](../flows/strategy-condition-trees.md)
- [`docs/flows/strategy-indicators-catalog.md`](../flows/strategy-indicators-catalog.md)

---

## Purpose

Extend strategies with **description**, read-only **PnL / drawdown** stubs, **AND/OR condition trees** that lead to an action, a **shared + DB indicator catalog** (no calculation yet), normalized **rules / conditions / indicators** tables with FKs, and a UI editor for multi-rule trees.

---

## Catalog lock

### Indicators (code + `indicators` table)

| Code | Parameters |
| --- | --- |
| `sma` | `period` |
| `ema` | `period` |
| `rsi` | `period` |
| `bollinger_bands` | `period`, `stdDev` |
| `macd` | `fast`, `slow`, `signal` |

`calculation_rule_json` is NULL (reserved).

### Condition nodes

| Wire `type` | Domain |
| --- | --- |
| `and` / `or` | `And` / `Or` (non-empty children) |
| `price_compare` | `PriceCompare(instrument, operator, threshold)` |
| `indicator_compare` | `IndicatorCompare(indicatorId, parameters, operator, threshold)` |

Operators: `lt` \| `lte` \| `gt` \| `gte` \| `eq`

### Actions

| Wire | Domain |
| --- | --- |
| `hold` / `buy` / `sell` | existing |
| `open_lp` | `OpenLp(instrumentPair, allocationPercent, yearlyFeePercent)` |

---

## Schema

- `indicators` — global catalog
- `strategies` — add `description`; drop `definition_json`
- `strategy_rules` — FK strategy
- `strategy_rule_conditions` — tree rows; `indicator_id` FK for indicator leaves

---

## HTTP / WS contract (breaking)

```text
CreateStrategyHttpRequest
  name, description
  rules: [{ id, when: ConditionWire, then: ActionWire }]

ConditionWire
  type, children?, instrument?, indicator?, operator?, threshold?, parameters?

ActionWire
  type, instrument?, instrumentPair?, allocationPercent?, yearlyFeePercent?

Get detail also returns pnl / drawdown as empty stubs (pending).
```

---

## E2E scenarios

### T1 — Create with AND(SMA lt, price gt) → open_lp

**Given** valid tree + open_lp action  
**When** create  
**Then** persisted rules/conditions with indicator FK; detail round-trips tree

### T2 — Reject unknown indicator

**Given** `indicator_compare` with `indicator: "nope"`  
**When** create  
**Then** 400 / failed; nothing persisted

### T3 — Reject empty AND

**Given** `when.type=and` with empty children  
**When** create  
**Then** rejected

### T4 — Update replaces rule graph

**Given** existing strategy  
**When** update description + new OR tree  
**Then** version bumps; old condition rows replaced

### T5 — UI create with description + AND tree

**Given** signed-in user  
**When** create via UI  
**Then** list shows strategy; edit shows pending PnL/drawdown

---

## Out of scope

- Indicator calculation / filling `calculation_rule_json`
- Real PnL / drawdown
- NOT / XOR
- Privacy / version history UI
