# Strategy reads (list + get)

## Purpose

Owner-scoped read APIs: paginated/sorted list and single-strategy detail, without exposing other owners’ private strategies.

## Actors

- `bootstrap` GET handlers + `ApplicationRoutes`
- `strategy.adapter.web.StrategyRestAdapter`
- `strategy.application.ListStrategies` / `GetStrategy`
- `strategy.adapter.persistence.InMemoryStrategyRepository`
- `shared.infra.http` (query map, path template match, Jetty)

## Sequence

```mermaid
sequenceDiagram
    participant Client
    participant Jetty as JettyRouteDispatchHandler
    participant Handler as ListOrGetHandler
    participant Adapter as StrategyRestAdapter
    participant App as ListStrategies_or_GetStrategy
    participant Repo as StrategyRepository

    Client->>Jetty: GET /strategies?... or GET /strategies/id?ownerId=
    Jetty->>Handler: HttpRequest with query and pathVariables
    Handler->>Adapter: list or get
    Adapter->>App: execute(query)
    App->>Repo: listByOwner or get
    Adapter-->>Client: 200 JSON or 400/404
```

## Walkthrough

1. Jetty parses query string and matches exact or `{strategyId}` template routes.
2. List handler builds `ListStrategiesQuery` (defaults + validation).
3. Repository filters by owner, sorts, slices page, returns total.
4. Get loads by id and requires matching `ownerId`; otherwise adapter returns `404`.

## Errors

| Case | Surface |
| --- | --- |
| Blank owner | `400` |
| Invalid page/size/sort/order | `400` |
| Missing / foreign strategy | `404` |
