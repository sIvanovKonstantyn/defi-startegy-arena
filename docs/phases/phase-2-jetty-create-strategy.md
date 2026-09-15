# Phase 2 — Jetty POST /strategies

**Status:** implemented  
**Depends on:** [phase-1-strategy-create.md](./phase-1-strategy-create.md)

## Purpose

Expose phase-1 create-strategy through the real HTTP edge: `POST /strategies` on Jetty, wired only at the composition root (`bootstrap`), without letting hexagonal layers depend on `shared.infra`.

## In scope

| Item | Detail |
| --- | --- |
| Route | `POST /strategies` |
| Binding | JSON body → `CreateStrategyHttpRequest` → `StrategyRestAdapter` |
| Composition | `ApplicationComposition` wires in-memory repo + event publisher + adapter |
| HTTP e2e | Start Jetty on ephemeral port; assert status + JSON + persistence |

## Out of scope

- Authn/authz headers
- Additional DSL types
- Persistent DB

## E2E scenarios

### H1 — Create via Jetty

**Given** a valid JSON body  
**When** `POST /strategies`  
**Then** HTTP `201`, JSON contains non-blank `strategyId`, strategy is persisted

### H2 — Duplicate via Jetty

**Given** the same owner+name already created  
**When** `POST /strategies` again  
**Then** HTTP `409`, still one persisted strategy

### H3 — Invalid JSON via Jetty

**When** body is not valid JSON  
**Then** HTTP `400`

Flow: [`docs/flows/strategy-create-http.md`](../flows/strategy-create-http.md)
