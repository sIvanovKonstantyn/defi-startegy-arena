package com.defistrategyarena.strategy.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.shared.kernel.IdempotentUuid;

public record StrategyId(String value) {

    private static final String MUST_NOT_BE_BLANK = "strategy id must not be blank";

    public StrategyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public static StrategyId create(IdGenerationInput input) {
        return new StrategyId(IdempotentUuid.from(input).toString());
    }
}
