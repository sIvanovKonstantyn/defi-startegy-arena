package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Objects;
import java.util.Optional;

public final class DeleteStrategy {

    private static final String COMMAND_REQUIRED = "delete strategy command must not be null";

    private final StrategyRepository strategies;

    public DeleteStrategy(DeleteStrategyDeps deps) {
        this.strategies = deps.strategies();
    }

    public Optional<StrategyId> execute(DeleteStrategyCommand command) {
        Objects.requireNonNull(command, COMMAND_REQUIRED);
        DeleteStrategyCommand validated = DeleteStrategyCommand.create(command);
        Optional<Strategy> owned =
                OwnedStrategyLookup.find(
                        new OwnedStrategyLookup.OwnedStrategyLookupQuery(
                                strategies, validated.ownerId(), validated.strategyId()));
        if (owned.isEmpty()) {
            return Optional.empty();
        }
        StrategyId id = owned.get().id();
        strategies.delete(id);
        return Optional.of(id);
    }

    public record DeleteStrategyDeps(StrategyRepository strategies) {}
}
