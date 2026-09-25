package com.defistrategyarena.strategy.adapter.dsl;

public record StrategyActionWire(
        String type,
        String instrument,
        String instrumentPair,
        String allocationPercent,
        String yearlyFeePercent) {

    public StrategyActionWire {
        type = StrategyWireText.orEmpty(new StrategyWireText.TextValue(type));
        instrument = StrategyWireText.orEmpty(new StrategyWireText.TextValue(instrument));
        instrumentPair = StrategyWireText.orEmpty(new StrategyWireText.TextValue(instrumentPair));
        allocationPercent = StrategyWireText.orEmpty(new StrategyWireText.TextValue(allocationPercent));
        yearlyFeePercent = StrategyWireText.orEmpty(new StrategyWireText.TextValue(yearlyFeePercent));
    }
}
