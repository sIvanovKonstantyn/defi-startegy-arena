# Example package shape: `strategy`

Illustrative layout for **one** bounded context under the agreed architecture (no runtime framework yet). This is a **shape example**, not implemented production code.

Goal of the slice: *user creates a private strategy version and later submits it to the arena* (submit itself lives in `arena`; `strategy` only owns definition + privacy + emits an event when a version is published).

---

## Package tree

```text
com.defistrategyarena.strategy
  domain/
    StrategyId.java
    StrategyDefinition.java          # DSL AST (record tree)
    Strategy.java                    # aggregate: id, owner, versions, privacy
    StrategyVersion.java
    Privacy.java                     # PRIVATE | SHARED …
    StrategyDomainService.java       # pure rules: version immutability, privacy
  application/
    CreateStrategy.java              # use case
    PublishStrategyVersion.java
    CreateStrategyCommand.java       # DTO (record)
    PublishStrategyVersionCommand.java
    StrategyRepository.java          # outbound port (interface)
    StrategyQueries.java             # read port if needed
  adapter/
    persistence/
      InMemoryStrategyRepository.java   # or JDBC later — behind the port
    # web/  (later, when HTTP runtime is chosen)
      # StrategyController.java
com.defistrategyarena.shared.messaging
  DomainEvent.java
  DomainEventPublisher.java
  DomainEventListener.java
com.defistrategyarena.shared.events.strategy
  StrategyVersionPublished.java      # integration event (other contexts may listen)
```

Cross-context: `arena` must **not** import `strategy.domain`. It listens to `StrategyVersionPublished` (in `shared`) and loads what it needs via its own models / ids carried on the event.

---

## Domain (pure)

```java
package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.shared.kernel.IdempotentUuid;

public record StrategyId(String value) {
    private static final String MUST_NOT_BE_BLANK = "strategy id must not be blank";

    public StrategyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public static StrategyId create(IdGenerationInput input) {
        return new StrategyId(IdempotentUuid.from(input).toString());
    }
}
```

```java
package com.defistrategyarena.strategy.domain;

public enum Privacy {
    PRIVATE,
    SHARED
}
```

```java
package com.defistrategyarena.strategy.domain;

import java.util.List;

public record StrategyDefinition(String name, List<Rule> rules) {
    public record Rule(String id, Condition when, Action then) {}
    public sealed interface Condition permits PriceAbove, IndicatorBelow {}
    public record PriceAbove(String instrument, String threshold) implements Condition {}
    public record IndicatorBelow(String indicator, String threshold) implements Condition {}
    public sealed interface Action permits Buy, Sell, Hold {}
    public record Buy(String instrument, String allocationPercent) implements Action {}
    public record Sell(String instrument, String allocationPercent) implements Action {}
    public record Hold() implements Action {}

    public static StrategyDefinition create(StrategyDefinition draft) {
        return draft;
    }
}
```

Note: top-level domain types need `create(...)`. Nested DSL nodes (`Rule`, `Buy`, …) are excluded from the ArchUnit rule. Prefer generating entity/aggregate ids with `IdempotentUuid`; pure value copies may use `create` as a normalizing factory.

```java
package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;

public final class Strategy {
    private final StrategyId id;
    private final String ownerId;
    private Privacy privacy;
    private StrategyVersion current;

    private Strategy(StrategyId id, String ownerId, StrategyDefinition definition) {
        this.id = id;
        this.ownerId = ownerId;
        this.privacy = Privacy.PRIVATE;
        this.current = StrategyVersion.initial(definition);
    }

    public static Strategy create(CreateStrategyData data) {
        StrategyId id =
                StrategyId.create(
                        IdGenerationInput.create(
                                new IdGenerationInput.StringListFields(
                                        java.util.List.of(data.ownerId(), data.definition().name()))));
        return new Strategy(id, data.ownerId(), data.definition());
    }

    public record CreateStrategyData(String ownerId, StrategyDefinition definition) {}

    public StrategyVersion publishNewVersion(StrategyDefinition definition) {
        this.current = this.current.next(definition);
        return this.current;
    }

    public StrategyId id() { return id; }
    public String ownerId() { return ownerId; }
    public Privacy privacy() { return privacy; }
    public StrategyVersion current() { return current; }
}
```

Notes:

- No framework imports in `domain`.
- Prefer **records** for values / DSL nodes.
- **IDs**: `IdempotentUuid.from(IdGenerationInput)` — same fields ⇒ same UUID.
- ArchUnit: every top-level concrete `..domain..` type has `public static create(...)` returning itself; non-records have **only private constructors**.

---

## Application (use cases + ports)

One command DTO per use case (satisfies “at most one parameter, and it is a DTO”):

```java
package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.StrategyDefinition;

public record CreateStrategyCommand(String ownerId, StrategyDefinition definition) {}
```

```java
package com.defistrategyarena.strategy.application;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;

public final class CreateStrategy {
    private final StrategyRepository strategies;
    private final DomainEventPublisher events;

    public CreateStrategy(CreateStrategyDeps deps) {
        this.strategies = deps.strategies();
        this.events = deps.events();
    }

    public StrategyId execute(CreateStrategyCommand command) {
        Strategy strategy =
                Strategy.create(new Strategy.CreateStrategyData(command.ownerId(), command.definition()));
        strategies.save(strategy);
        events.publish(new StrategyVersionPublished(
                strategy.id().value(),
                strategy.current().number(),
                command.ownerId()));
        return strategy.id();
    }

    public record CreateStrategyDeps(StrategyRepository strategies, DomainEventPublisher events) {}
}
```

Outbound port (implemented only in `adapter`):

```java
package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;

public interface StrategyRepository {
    StrategyId nextId();
    void save(Strategy strategy);
    Strategy get(StrategyId id);
}
```

---

## Integration event (in `shared`, not in `strategy`)

```java
package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;

public record StrategyVersionPublished(
        String strategyId,
        int versionNumber,
        String ownerId
) implements DomainEvent {}
```

Listener in **another** context (e.g. `arena.application`) — never imports `strategy.*`:

```java
package com.defistrategyarena.arena.application;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEventListener;

public final class OnStrategyVersionPublished
        implements DomainEventListener<StrategyVersionPublished> {

    @Override
    public void on(StrategyVersionPublished event) {
        // arena records that a version exists / invalidates cache / etc.
    }
}
```

---

## Adapter (infrastructure)

```java
package com.defistrategyarena.strategy.adapter.persistence;

import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStrategyRepository implements StrategyRepository {
    private final Map<String, Strategy> store = new ConcurrentHashMap<>();

    @Override
    public StrategyId nextId() {
        return new StrategyId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Strategy strategy) {
        store.put(strategy.id().value(), strategy);
    }

    @Override
    public Strategy get(StrategyId id) {
        return store.get(id.value());
    }
}
```

HTTP adapter appears later as `adapter.web` once a runtime is chosen; domain/application stay unchanged.

See **REST e2e shape**: [`strategy-rest-e2e.md`](./strategy-rest-e2e.md) — e2e enters through `StrategyRestAdapter.create(CreateStrategyHttpRequest)`.

---

## Tests (per delivery rule)

```text
src/test/java/com/defistrategyarena/strategy/
  e2e/
    CreateStrategyE2ETest.java     # approved scenarios — call REST adapter
  unit/
    StrategyTest.java              # edges: privacy, versioning
```

E2E talks to the **REST input adapter** (HTTP-shaped DTOs) with in-memory outbound adapters — **context-level** e2e, not browser e2e.

---

## How this maps to ArchUnit

| Rule | How this example obeys it |
| --- | --- |
| Hexagonal | `domain` ← `application` ← `adapter` |
| No context→context deps | `arena` only sees `shared.events.strategy` |
| Async between contexts | `DomainEventPublisher` / `DomainEventListener` |
| Records / no Lombok | DSL + commands as records |
| One DTO param | `execute(CreateStrategyCommand)` |
| Framework-free domain | no servlet/JPA/Spring in `domain` |

---

## Fit check (questions for us)

1. Is putting **integration events under `shared.events.<context>`** the right shared language, or should event payloads be even thinner (ids only)?
2. Is **emitting `StrategyVersionPublished` on create** correct, or only on explicit “publish/submit”?
3. Should `ownerId` be a proper `identity` type in `shared`, or stay a string until Identity context exists?

If this shape feels right, the next step is an approved e2e scenario list for `strategy` (or `arena`) and then real implementation under TDD.

**Phase 1 approval package:** [`docs/phases/phase-1-strategy-create.md`](../phases/phase-1-strategy-create.md) · flow: [`docs/flows/strategy-create.md`](../flows/strategy-create.md).
