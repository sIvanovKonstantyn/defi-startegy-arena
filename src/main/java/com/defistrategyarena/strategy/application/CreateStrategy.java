package com.defistrategyarena.strategy.application;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Objects;

public final class CreateStrategy {

    private static final String COMMAND_REQUIRED = "create strategy command must not be null";

    private final StrategyRepository strategies;
    private final DomainEventPublisher events;

    public CreateStrategy(CreateStrategyDeps deps) {
        this.strategies = deps.strategies();
        this.events = deps.events();
    }

    public StrategyId execute(CreateStrategyCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        Strategy strategy =
                Strategy.create(new Strategy.CreateStrategyData(command.ownerId(), command.definition()));
        strategies.save(strategy);
        events.publish(
                StrategyVersionPublished.create(
                        new StrategyVersionPublished(
                                strategy.id().value(),
                                strategy.current().number(),
                                strategy.ownerId())));
        return strategy.id();
    }

    public record CreateStrategyDeps(StrategyRepository strategies, DomainEventPublisher events) {}
}
