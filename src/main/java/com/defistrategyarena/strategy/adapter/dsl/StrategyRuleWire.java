package com.defistrategyarena.strategy.adapter.dsl;

public record StrategyRuleWire(
        String id, StrategyConditionWire when, StrategyActionWire then) {}
