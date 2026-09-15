package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import java.util.Optional;

public final class GetStrategy {

    private final StrategyRepository strategies;

    public GetStrategy(GetStrategyDeps deps) {
        this.strategies = deps.strategies();
    }

    public Optional<Strategy> execute(GetStrategyQuery query) {
        GetStrategyQuery validated = GetStrategyQuery.create(query);
        Optional<Strategy> found = strategies.get(validated.strategyId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Strategy strategy = found.get();
        if (!validated.ownerId().equals(strategy.ownerId())) {
            return Optional.empty();
        }
        return Optional.of(strategy);
    }

    public record GetStrategyDeps(StrategyRepository strategies) {}
}
