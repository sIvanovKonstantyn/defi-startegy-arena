package com.defistrategyarena.strategy.domain;

import java.util.Locale;
import java.util.Map;

public enum CompareOperator {
    LT,
    LTE,
    GT,
    GTE,
    EQ;

    private static final String UNKNOWN_OPERATOR = "unknown compare operator";
    private static final String EMPTY = "";
    private static final Map<String, CompareOperator> BY_WIRE = Map.of(
            LT.wireValue(), LT,
            LTE.wireValue(), LTE,
            GT.wireValue(), GT,
            GTE.wireValue(), GTE,
            EQ.wireValue(), EQ);

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CompareOperator fromWire(WireOperator wire) {
        String raw = wire.value() == null ? EMPTY : wire.value().trim();
        CompareOperator operator = BY_WIRE.get(raw.toLowerCase(Locale.ROOT));
        if (operator == null) {
            throw new IllegalArgumentException(UNKNOWN_OPERATOR);
        }
        return operator;
    }

    public record WireOperator(String value) {}
}
