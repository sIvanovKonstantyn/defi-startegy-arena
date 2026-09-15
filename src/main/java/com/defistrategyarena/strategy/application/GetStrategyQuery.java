package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.StrategyId;

public record GetStrategyQuery(String ownerId, StrategyId strategyId) {

    private static final String OWNER_REQUIRED = "owner id must not be blank";
    private static final String STRATEGY_ID_REQUIRED = "strategy id must not be null";

    public GetStrategyQuery {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException(OWNER_REQUIRED);
        }
        if (strategyId == null) {
            throw new IllegalArgumentException(STRATEGY_ID_REQUIRED);
        }
    }

    public static GetStrategyQuery create(GetStrategyQuery draft) {
        return new GetStrategyQuery(draft.ownerId(), draft.strategyId());
    }
}
