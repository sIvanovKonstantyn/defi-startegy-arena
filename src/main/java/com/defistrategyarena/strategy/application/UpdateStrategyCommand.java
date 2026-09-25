package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.NonEmptyRuleList;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.List;

public record UpdateStrategyCommand(
        String ownerId,
        StrategyId strategyId,
        String description,
        List<StrategyDefinition.Rule> rules) {

    private static final String OWNER_REQUIRED = "owner id must not be blank";
    private static final String STRATEGY_ID_REQUIRED = "strategy id must not be null";
    private static final String DESCRIPTION_REQUIRED = "description must not be null";

    public UpdateStrategyCommand {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException(OWNER_REQUIRED);
        }
        if (strategyId == null) {
            throw new IllegalArgumentException(STRATEGY_ID_REQUIRED);
        }
        if (description == null) {
            throw new IllegalArgumentException(DESCRIPTION_REQUIRED);
        }
        rules = NonEmptyRuleList.copyRequired(rules);
    }

    public static UpdateStrategyCommand create(UpdateStrategyCommand draft) {
        return new UpdateStrategyCommand(
                draft.ownerId(), draft.strategyId(), draft.description(), draft.rules());
    }
}
