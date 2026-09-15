package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.StrategyDefinition;

public record CreateStrategyCommand(String ownerId, StrategyDefinition definition) {

    public static CreateStrategyCommand create(CreateStrategyCommand draft) {
        return new CreateStrategyCommand(draft.ownerId(), draft.definition());
    }
}
