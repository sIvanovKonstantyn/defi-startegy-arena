package com.defistrategyarena.strategy.application;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.domain.Strategy;
import java.util.Objects;
import java.util.Optional;

public final class UpdateStrategy {

    private static final String COMMAND_REQUIRED = "update strategy command must not be null";

    private final StrategyRepository strategies;
    private final DomainEventPublisher events;

    public UpdateStrategy(UpdateStrategyDeps deps) {
        this.strategies = deps.strategies();
        this.events = deps.events();
    }

    public Optional<UpdateStrategyResult> execute(UpdateStrategyCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        UpdateStrategyCommand validated = UpdateStrategyCommand.create(command);
        return OwnedStrategyLookup.find(
                        new OwnedStrategyLookup.OwnedStrategyLookupQuery(
                                strategies, validated.ownerId(), validated.strategyId()))
                .map(existing -> publishAndPersist(new PublishPersistInput(existing, validated)));
    }

    private UpdateStrategyResult publishAndPersist(PublishPersistInput input) {
        Strategy updated =
                input.existing()
                        .publishNewVersion(
                                new Strategy.PublishNewVersionData(
                                        input.command().description(), input.command().rules()));
        strategies.update(updated);
        events.publish(
                StrategyVersionPublished.create(
                        new StrategyVersionPublished(
                                updated.id().value(),
                                updated.current().number(),
                                updated.ownerId())));
        return new UpdateStrategyResult(updated.id(), updated.current().number());
    }

    private record PublishPersistInput(Strategy existing, UpdateStrategyCommand command) {}

    public record UpdateStrategyDeps(StrategyRepository strategies, DomainEventPublisher events) {}
}
