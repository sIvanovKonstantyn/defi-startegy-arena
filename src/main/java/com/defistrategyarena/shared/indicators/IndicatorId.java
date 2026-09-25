package com.defistrategyarena.shared.indicators;

public record IndicatorId(String value) {

    private static final String MUST_NOT_BE_BLANK = "indicator id must not be blank";

    public IndicatorId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public static IndicatorId create(IndicatorId draft) {
        return new IndicatorId(draft.value());
    }
}
