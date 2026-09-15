# Create strategy over HTTP (Jetty)

## Purpose

Accept `POST /strategies` JSON at the Jetty edge, map to `StrategyRestAdapter`, and return an HTTP-shaped JSON response — without importing Jetty or `shared.infra` into the strategy hexagonal layers.

## Actors

- `bootstrap.ApplicationRoutes` / `CreateStrategyHttpHandler`
- `shared.http.handlers.BaseHandler` (generic JSON read/execute/write + bad-request body)
- `bootstrap.ApplicationComposition`
- `strategy.adapter.web.StrategyRestAdapter`
- `shared.infra.http` + Jetty bootstrap
- `shared.messaging.InMemoryDomainEventPublisher`

## Sequence

```mermaid
sequenceDiagram
    participant Client
    participant Jetty as JettyRouteDispatchHandler
    participant Route as CreateStrategyHttpHandler
    participant Rest as StrategyRestAdapter
    participant App as CreateStrategy
    participant Repo as StrategyRepository

    Client->>Jetty: POST /strategies JSON
    Jetty->>Route: handle(HttpRequest)
    Route->>Rest: create(CreateStrategyHttpRequest)
    Rest->>App: execute(command)
    App->>Repo: save(strategy)
    App-->>Rest: StrategyId
    Rest-->>Route: CreateStrategyHttpResponse
    Route-->>Client: HTTP status + JSON body
```

## Walkthrough

1. Composition root builds `ApplicationComposition` (repo, publisher, adapter).
2. `ApplicationRoutes` registers `POST /strategies` to `CreateStrategyHttpHandler` (extends `BaseHandler<Req,Res>`).
3. `BaseHandler` deserializes JSON into `CreateStrategyHttpRequest`; on failure it serializes `badRequestBody()`.
4. Handler `execute` delegates to `StrategyRestAdapter.create` (phase-1 behavior unchanged).
5. `BaseHandler` serializes the `JsonHttpResult` and returns its `status` + `application/json`.

## Errors / edge cases

| Case | Surface |
| --- | --- |
| Invalid JSON | `400` + empty strategy id JSON |
| Duplicate owner+name | `409` from adapter |
| Validation failures | `400` from adapter |
