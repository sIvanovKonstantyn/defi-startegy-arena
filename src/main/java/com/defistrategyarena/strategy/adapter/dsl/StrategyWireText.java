package com.defistrategyarena.strategy.adapter.dsl;

public final class StrategyWireText {

    private StrategyWireText() {}

    public static String orEmpty(TextValue text) {
        if (text.value() == null) {
            return StrategyDslWireNames.EMPTY;
        }
        return text.value();
    }

    public record TextValue(String value) {}
}
