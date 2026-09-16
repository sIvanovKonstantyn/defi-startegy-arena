package com.defistrategyarena.strategy.adapter.dsl;

public record StrategyRuleWire(
        String id,
        String conditionType,
        String actionType,
        String instrument,
        String indicator,
        String threshold,
        String allocationPercent) {}
