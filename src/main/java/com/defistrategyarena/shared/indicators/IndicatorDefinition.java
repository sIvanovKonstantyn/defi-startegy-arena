package com.defistrategyarena.shared.indicators;

import java.util.List;

public record IndicatorDefinition(
        IndicatorId id, String displayName, List<IndicatorParameterSpec> parameters) {

    private static final String ID_REQUIRED = "indicator id must not be null";
    private static final String DISPLAY_REQUIRED = "display name must not be blank";
    private static final String PARAMS_REQUIRED = "parameters must not be null";

    public IndicatorDefinition {
        if (id == null) {
            throw new IllegalArgumentException(ID_REQUIRED);
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(DISPLAY_REQUIRED);
        }
        if (parameters == null) {
            throw new IllegalArgumentException(PARAMS_REQUIRED);
        }
        parameters = List.copyOf(parameters);
    }

    public static IndicatorDefinition create(IndicatorDefinition draft) {
        return new IndicatorDefinition(draft.id(), draft.displayName(), draft.parameters());
    }
}
