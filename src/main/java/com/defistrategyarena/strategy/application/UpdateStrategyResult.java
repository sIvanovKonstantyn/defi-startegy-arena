package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.StrategyId;

public record UpdateStrategyResult(StrategyId strategyId, int versionNumber) {

    public static UpdateStrategyResult create(UpdateStrategyResult draft) {
        return new UpdateStrategyResult(draft.strategyId(), draft.versionNumber());
    }
}
