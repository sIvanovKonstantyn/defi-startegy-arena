package com.defistrategyarena.strategy.application;

public record StrategyUseCases(
        CreateStrategy createStrategy,
        ListStrategies listStrategies,
        GetStrategy getStrategy,
        UpdateStrategy updateStrategy,
        DeleteStrategy deleteStrategy) {}
