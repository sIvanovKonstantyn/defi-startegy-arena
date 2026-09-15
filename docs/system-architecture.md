# DeFi Strategy Arena — System Architecture

Architecture for **DeFi Strategy Arena**: users define highly customizable trading/DeFi strategies, submit them against a market period, the platform backtests on real historical data, scores PnL and related metrics, and publishes results to a leaderboard. Strategies are private by default, optionally shareable, and exportable in multiple formats.

This document defines the target shape of the system. Enforcement of package/module boundaries is done with ArchUnit (see [code-quality-and-architecture.md](./code-quality-and-architecture.md)).

---

## 1. Product forces that drive architecture

| Force | Architectural consequence |
| --- | --- |
| Strategies are *really* customizable (actions + triggers) | Strategy is a **declarative domain model / DSL**, not uploaded arbitrary code |
| Fair, reproducible leaderboard | Backtests run on **versioned, immutable market-data snapshots**; engine must be **deterministic** |
| Compute-heavy backtests | **Async job pipeline**; API accepts submission, workers execute, results published later |
| Strategies private unless shared | Strict **ownership + ACL**; leaderboard shows metrics (and optional share link), never strategy body by default |
| Export in multiple formats | Strategy and results exposed via **export ports**; format adapters stay outside the domain |
| DeFi protocols evolve | Protocol integrations are **adapters behind ports**; core engine speaks abstract actions (swap, lend, provide liquidity, …) |
| Java BE + React web (web may change) | **Package-based modulith** + hexagonal slices; UI is one adapter among many. **HTTP runtime: Jetty 12** behind `shared.infra` ports. |

---

## 2. Platform & style decisions

| Decision | Choice |
| --- | --- |
| Build / package manager | **Gradle** |
| Language | **Java 25** |
| Structure | **Package-based modulith** — single Gradle project, bounded contexts as **packages** (not multi-module JARs yet) |
| Style inside each context | Hexagonal (ports & adapters) |
| Runtime / web / DI framework | **Jetty 12** for HTTP (selected in `main` via `HttpServerBootstrap`); DI still TBD |

**Decision: start as a package-based modulith**, not Gradle multi-module and not a microservice mesh.

Why:

- One product, one consistency story for submissions → backtests → leaderboard
- Simpler ops and local agent workflows while domain is still settling
- Package boundaries + ArchUnit keep contexts honest without picking a framework early
- Contexts can later become separate Gradle modules or services **along the same package cuts**

**Inside each context package: hexagonal (ports & adapters).**

```text
                    ┌─────────────────────────────────────────┐
                    │              Adapters (in)               │
                    │  Web API  ·  CLI  ·  Admin  ·  Webhooks │
                    └───────────────────┬─────────────────────┘
                                        │
                    ┌───────────────────▼─────────────────────┐
                    │         Application services            │
                    │   use cases / orchestration / jobs      │
                    └───────────────────┬─────────────────────┘
                                        │
          ┌─────────────────────────────▼─────────────────────────────┐
          │                     Domain (pure)                          │
          │  Strategy DSL · Backtest engine contracts · Scoring · ACL │
          └─────────────────────────────┬─────────────────────────────┘
                                        │ ports
          ┌─────────────────────────────▼─────────────────────────────┐
          │                   Adapters (out)                           │
          │  DB · Market-data store · Queue · Object storage · Export │
          └───────────────────────────────────────────────────────────┘
```

Dependency rule (ArchUnit):

- `domain` → nothing outward
- `application` → `domain` only
- `adapters.*` → `application` / `domain` (implement ports)
- No reverse dependency from domain to frameworks, HTTP, JDBC, React, etc.
- Top-level concrete types in `..domain..` must expose **`public static create(...)`** returning themselves (ids via idempotent UUID from fields); non-record domain types have **private constructors only**

---

## 3. Bounded contexts (packages)

Organize the codebase as **top-level packages** inside one Gradle application. ArchUnit enforces walls between them. Do **not** introduce Gradle multi-module splits until a context needs an independent lifecycle.

```text
…identity          …strategy           …marketdata         …arena              …leaderboard
──────────        ──────────         ────────────       ─────              ───────────
accounts          definition DSL     instruments        submission         rankings
authn/authz       privacy / share    OHLCV / ticks      backtest runner    public metrics
                  export ports       indicators         PnL / risk stats   period boards
                                     snapshot versions  job lifecycle
```

### 3.1 Identity

- Users, sessions, API credentials
- Owns authentication; authorization decisions for strategy visibility are enforced in **Strategy** (resource owner) with identity as the subject

### 3.2 Strategy (studio)

Core product model for “what the strategy is.”

Responsibilities:

- Create / edit / version strategy definitions
- Validate DSL against a schema (actions, conditions, constraints)
- Privacy: **private by default**; explicit share (link, user, or public-read of definition)
- Export definition (and later: export with optional results bundle)

**Critical decision — declarative strategies, not user-supplied code:**

Users compose rules such as:

- **When** (triggers): price crosses X, indicator RSI &lt; N, time window, on-chain event (later), portfolio state
- **Then** (actions): buy / sell / hold, allocate %, interact with a named DeFi protocol capability (swap, lend, withdraw, …)
- **Constraints**: max drawdown stop, position limits, allowed venues/assets

Stored as a versioned document (canonical JSON in DB) mapped to a typed domain AST. The backtest engine **interprets** that AST. This keeps the sandbox safe, strategies portable, and exports deterministic.

Customizability grows by **extending the DSL and protocol adapters**, not by opening a scripting VM in v1.

### 3.3 Market data

Responsibilities:

- Ingest and store historical market series (CEX and/or DeFi pools — sources are adapters)
- Compute or store indicators needed by the DSL
- Publish **immutable snapshots** tagged with `snapshotId` + period (start/end) used by arena submissions

Leaderboard fairness depends on: same snapshot + same engine version + same strategy version ⇒ same result.

### 3.4 Arena (backtesting)

Responsibilities:

- Accept a **submission**: strategy version + market period + market-data snapshot (+ engine version)
- Queue and run backtests asynchronously
- Interpret strategy AST against the snapshot (bar/tick loop)
- Produce a **result** aggregate: equity curve, trades, PnL, fees, drawdown, sharpe-like stats, protocol-specific fills as modeled
- Persist results; notify leaderboard

Execution model:

```text
Submit ──► Validate ──► Enqueue job ──► Worker runs engine ──► Persist Result ──► Publish rank update
              │                │
              │                └── idempotent job id; retries; dead-letter for poison strategies
              └── reject invalid DSL / unsupported actions for this engine version
```

Engine principles:

- **Deterministic** given (strategyVersion, snapshotId, engineVersion, startingCapital, fee model)
- Time moves only via market-data events (no wall-clock in scoring)
- Fees, slippage, and funding modeled explicitly (even if simplified in v1) so later realism upgrades bump `engineVersion`

### 3.5 Leaderboard

Responsibilities:

- Rank submissions per market period / arena season by agreed metrics (PnL %, risk-adjusted, …)
- Show **public scorecards** without revealing private strategy bodies
- If a strategy is shared, leaderboard entries may deep-link to the shared definition

Leaderboard reads **results**, not live strategy drafts. Only **finalized successful runs** enter rankings (policy TBD: best run vs latest vs official submission slot).

---

## 4. Core domain objects (conceptual)

```text
User
Strategy ── versions[] ──► StrategyDefinition (AST)
   │ privacy: PRIVATE | SHARED | (optional PUBLIC_DEFINITION)
   │
Submission ──► { strategyVersionId, period, snapshotId, engineVersion, status }
   │
BacktestJob
   │
Result ──► metrics, equityCurve, fills, diagnostics
   │
LeaderboardEntry ──► { periodId, submissionId, rank, publicMetrics }
ShareGrant ──► { strategyId, grantee | link token, scope }
ExportRequest ──► { artifact, format } → bytes via export adapter
```

Privacy rule of thumb:

- **Definition** visible only to owner + explicit grants
- **Result metrics** may be public on the leaderboard even when definition stays private
- Sharing is intentional and audited (who shared what, when)

---

## 5. Strategy DSL shape (v1 direction)

Keep the DSL intentionally boring and enumerable.

```text
StrategyDefinition
  meta: name, baseAsset, quoteAsset, startingCapital rules
  universe: allowed instruments / protocols
  risk: max position, stop rules
  rules[]:
    id
    when: Condition AST   (and/or/not, comparators on price, indicators, portfolio)
    then: Action AST      (buy/sell/hold, protocol action refs)
    cooldown / priority
```

Extension points:

| Extension | Where it lives |
| --- | --- |
| New indicator | Market data + condition operators in domain |
| New trigger type | Domain condition node + engine evaluator |
| New DeFi protocol | Outbound adapter implementing `ProtocolPort` + action enum mapping |
| New export format | `Exporter` adapter (JSON, YAML, CSV trades, Markdown report, …) |

Avoid embedding raw SQL, JVM bytecode, or unrestricted scripts in v1.

---

## 6. Application flows

### 6.1 Author strategy

1. User edits definition in React studio
2. API validates against schema + semantic rules (unknown protocol, empty universe, …)
3. New **immutable version** stored; draft vs published versioning policy TBD
4. Definition never appears on leaderboard APIs unless share scope allows

### 6.2 Submit to arena

1. User picks period (and thus snapshot) + strategy version
2. System creates `Submission` + `BacktestJob`
3. Worker loads snapshot + definition, runs engine, writes `Result`
4. On success, leaderboard projection updates

### 6.3 Share & export

- **Share**: create `ShareGrant` (user-to-user or secret link); revoke support required
- **Export**: application service loads authorized definition/result and delegates to format-specific exporters

---

## 7. Technical building blocks (BE)

Locked defaults:

| Concern | Approach |
| --- | --- |
| Build | **Gradle** (single root project) |
| JDK | **Java 25** |
| Application style | Package-based modulith + hexagonal packages per context |
| Runtime / DI / HTTP | **Jetty 12** embedded via `shared.infra.http.jetty`; bootstrap selected in `DefiStrategyArenaApplication` (see [http-server-bootstrap flow](./flows/http-server-bootstrap.md)) |
| API | HTTP routes wired in `bootstrap.ApplicationRoutes`; context `adapter.web` registers handlers (React remains a client only) |
| Persistence | Behind ports; technology TBD |
| Jobs | Behind `JobPort`; technology TBD |
| Market data store | Behind `MarketDataPort`; technology TBD |
| Auth | Owned by Identity context; mechanism TBD |
| Observability | TBD with runtime |

React web:

- Strategy studio UI (form/visual builder over the DSL)
- Submission status
- Leaderboard
- Share / export actions  

Web talks **only** to application APIs — no direct DB or engine access.

---

## 8. Package map

Illustrative layout for ArchUnit:

```text
com.defistrategyarena
  identity/
  strategy/
    domain/
    application/
    adapter/web/
    adapter/persistence/
    adapter/export/
  marketdata/
    domain/
    application/
    adapter/ingestion/
    adapter/persistence/
  arena/
    domain/
    application/
    adapter/worker/
    adapter/persistence/
  leaderboard/
    domain/
    application/
    adapter/web/
    adapter/persistence/
  bootstrap/         # composition-root wiring (routes, future DI)
  shared/
    kernel/
    messaging/       # DomainEvent, DomainEventPublisher, DomainEventListener
    http/handlers/   # BaseHandler for resource HttpHandler implementations
    infra/           # HTTP runtime adapters (Jetty today; Helidon later)
      http/          # HttpServerBootstrap, HttpRouteRegistry, …
      http/jetty/    # Jetty-only code (confined by ArchUnit)
    events/          # cross-context integration events (add as needed)
```

**HTTP infra boundary:** Jetty/Helidon APIs may appear **only** under `shared.infra`. Bounded-context `domain` / `application` / `adapter` packages must not depend on `shared.infra`. The composition root (`DefiStrategyArenaApplication`, `bootstrap`) selects an `HttpServerBootstrap` implementation — it never imports Jetty/Helidon directly. Switching runtime later means changing `selectBootstrap()` only.

Each top-level context package is a **bounded context**. Cross-context communication:

- **Only asynchronous**, via `DomainEventPublisher` / `DomainEventListener` in `shared.messaging`
- Integration event types live under `shared` (so contexts never import each other)
- **No compile-time dependency** between `identity` / `strategy` / `marketdata` / `arena` / `leaderboard` (ArchUnit)
- Intra-context work may still use synchronous hexagonal calls (`application` → `domain` → ports)

---

## 9. Delivery constraint: context e2e first (TDD)

**Architecture / coding rule — mandatory for every bounded-context package:**

1. **Start from scenarios, not production code.** For a new context (or a new vertical slice inside one), first write the **context-level e2e scenarios** (happy paths and product-meaningful flows as seen from that context’s boundary — application facade or async event surface).
2. **User approval gate.** Scenario descriptions (and then the failing e2e tests that encode them) are reviewed and **approved by the user** before implementation work proceeds.
3. **Freeze approved e2e tests.** Once approved, those tests are implemented under the test source set and **must not be changed** to make implementation easier. If product intent changes, scenarios are re-approved explicitly; tests are updated only as a deliberate product change, not as a side effect of coding.
4. **Implement to green the e2e suite.** Production code’s first job is to satisfy the approved context e2e scenarios (classic outside-in TDD).
5. **Edge cases → unit tests.** Boundary conditions, error branches, and algorithmic details are covered by **unit tests** around domain/application units; they must not dilute or replace the frozen e2e contract.
6. **No drive-by e2e edits.** Agents and developers treat approved context e2e tests as a contract equivalent to ArchUnit rules.

Suggested test layout per context:

```text
src/test/java/…/<context>/
  e2e/           # approved context scenarios — frozen after user approval
  unit/          # edge cases, pure domain, adapters in isolation
  architecture/  # optional context-local ArchUnit extras
```

“E2E” here means **end-to-end from the bounded-context perspective** (context boundary + persistence/fakes as needed), not necessarily a full browser UI test. Full-system UI e2e can come later as a separate layer.

---

## 10. Security & fairness

- Never execute user-provided code on workers
- Rate-limit submissions; cap backtest simulation cost (bars × rules × protocols)
- Snapshot + engine version immutability for ranked runs
- Private strategies encrypted at rest if threat model requires (decision at implementation); always access-checked
- Shared links: unguessable tokens, expiry, revoke
- Export endpoints re-check authorization

---

## 11. What we deliberately postpone

- Live paper trading / real execution (architecture leaves a future `ExecutionPort`; arena remains historical first)
- Unrestricted user scripts (Python/JS) — only if a hardened sandbox is introduced later
- Gradle multi-module or microservice split — extract **arena workers** or **marketdata** first if load demands it; keep package cuts stable
- Multi-chain indexing complexity — introduce protocol adapters incrementally

---

## 12. Alignment with quality tooling

| Tool | Role here |
| --- | --- |
| ArchUnit | Package/context and layer dependency rules matching §2 and §8 |
| PMD | Complexity/size on engine and DSL validators especially |
| JaCoCo | High coverage floors on `strategy.domain` and `arena.domain`; e2e + unit both count |
| Context e2e tests | Frozen product contract per §9; primary driver of implementation |
| Unit tests | Edge cases only after / beside e2e; never a substitute for approved scenarios |

---

## 13. Next steps

1. ~~**Phase 1:** create-private-strategy slice~~ — done ([phase-1](./phases/phase-1-strategy-create.md)).
2. ~~**Phase 2:** Jetty `POST /strategies`~~ — done ([phase-2](./phases/phase-2-jetty-create-strategy.md), [flow](./flows/strategy-create-http.md)).
3. ~~**Phase 3:** V1 DSL condition/action catalog~~ — done ([phase-3](./phases/phase-3-dsl-catalog.md), [flow](./flows/strategy-dsl-catalog.md)).
4. ~~**Phase 4:** list/get strategies (paginated)~~ — done ([phase-4](./phases/phase-4-strategy-reads.md), [flow](./flows/strategy-reads.md)).
5. Define market-data snapshot format and one ingestion adapter.
6. Implement interpreter MVP and one leaderboard metric.
7. Add privacy/share and JSON/YAML export before broader protocol actions.
