package com.defistrategyarena.strategy.application;

public final class ListStrategies {

    private final StrategyRepository strategies;

    public ListStrategies(ListStrategiesDeps deps) {
        this.strategies = deps.strategies();
    }

    public StrategyPage execute(ListStrategiesQuery query) {
        return strategies.listByOwner(ListStrategiesQuery.create(query));
    }

    public record ListStrategiesDeps(StrategyRepository strategies) {}
}
