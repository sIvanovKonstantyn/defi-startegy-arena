package com.defistrategyarena.shared.indicators;

public record IndicatorParameterSpec(
        String name, ParameterType type, boolean required, String defaultValue) {

    private static final String NAME_REQUIRED = "parameter name must not be blank";
    private static final String TYPE_REQUIRED = "parameter type must not be null";
    private static final String DEFAULT_REQUIRED = "default value must not be null";

    public IndicatorParameterSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        if (type == null) {
            throw new IllegalArgumentException(TYPE_REQUIRED);
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException(DEFAULT_REQUIRED);
        }
    }

    public static IndicatorParameterSpec create(IndicatorParameterSpec draft) {
        return new IndicatorParameterSpec(
                draft.name(), draft.type(), draft.required(), draft.defaultValue());
    }

    public enum ParameterType {
        INTEGER,
        DECIMAL
    }
}
