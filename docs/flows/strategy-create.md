# Create private strategy

## Purpose

Accept an HTTP-shaped create request at the strategy context boundary, build a private strategy aggregate, persist it under a **unique `(ownerId, name)`** constraint, and publish `StrategyVersionPublished` only when insert succeeds — so other contexts can react without importing `strategy.*`.

## Actors

- `strategy.adapter.web` — `StrategyRestAdapter`
- `strategy.application` — `CreateStrategy`
- `strategy.domain` — `Strategy` / `StrategyDefinition`
- `strategy.adapter.persistence` — `StrategyRepository` implementation (in-memory in e2e; enforces unique owner+name)
- `shared.messaging` — `DomainEventPublisher`
- `shared.events.strategy` — `StrategyVersionPublished`

## Sequence (happy path)

```mermaid
sequenceDiagram
    participant Client as Adapter client / e2e
    participant Rest as StrategyRestAdapter
    participant App as CreateStrategy
    participant Dom as Strategy.create
    participant Repo as StrategyRepository
    participant Bus as DomainEventPublisher

    Client->>Rest: create(CreateStrategyHttpRequest)
    Rest->>App: execute(CreateStrategyCommand)
    App->>Dom: create(CreateStrategyData)
    Dom-->>App: Strategy (PRIVATE, version 1)
    App->>Repo: save(strategy)
    Note over Repo: unique index (ownerId, name)
    Repo-->>App: ok
    App->>Bus: publish(StrategyVersionPublished)
    App-->>Rest: StrategyId
    Rest-->>Client: CreateStrategyHttpResponse(201, strategyId)
```

## Sequence (duplicate owner + name)

```mermaid
sequenceDiagram
    participant Client as Adapter client / e2e
    participant Rest as StrategyRestAdapter
    participant App as CreateStrategy
    participant Repo as StrategyRepository
    participant Bus as DomainEventPublisher

    Client->>Rest: create(same ownerId + name)
    Rest->>App: execute(CreateStrategyCommand)
    App->>Repo: save(strategy)
    Repo-->>App: unique constraint violation
    Note over App,Bus: no StrategyVersionPublished
    App-->>Rest: duplicate failure
    Rest-->>Client: CreateStrategyHttpResponse(409, "")
```

## Walkthrough

1. Client (e2e or future Jetty handler) calls `StrategyRestAdapter.create` with owner, name, and optional rules.
2. Adapter maps HTTP DTOs to `CreateStrategyCommand` / domain `StrategyDefinition` (phase 1: `price_above` → hold).
3. `CreateStrategy` asks `Strategy.create(...)`, which assigns a `StrategyId` (idempotent UUID from fields is fine for a first insert).
4. Aggregate is saved through `StrategyRepository`. The adapter/DB **must** reject a second row with the same `(ownerId, name)`.
5. On success only: `StrategyVersionPublished` is published with strategy id, version number `1`, and owner id; adapter returns `201` + `strategyId`. E2E must reload the aggregate from the repository and assert it matches the request.
6. On unique-key conflict: adapter returns `409`; store unchanged (still one row for that owner+name); no extra event.

## Errors / edge cases

| Case | Surface |
| --- | --- |
| Blank owner or name | Validation failure before persist / event |
| Unknown rule `type` | Validation failure at adapter or application boundary |
| Repeat create same owner+name | Persistence unique constraint → HTTP-shaped `409`; store unchanged; no new event |

Product scenarios: [`docs/phases/phase-1-strategy-create.md`](../phases/phase-1-strategy-create.md).
