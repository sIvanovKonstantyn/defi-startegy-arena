package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;
import java.util.List;

public record StrategyDetailHttpResponse(
        int status,
        String strategyId,
        String name,
        String description,
        String privacy,
        int versionNumber,
        List<CreateStrategyHttpRequest.RuleBody> rules,
        String pnl,
        String drawdown)
        implements JsonHttpResult {

    private static final String RULES_REQUIRED = "rules must not be null";

    public StrategyDetailHttpResponse {
        if (rules == null) {
            throw new IllegalArgumentException(RULES_REQUIRED);
        }
        rules = List.copyOf(rules);
    }

    public static StrategyDetailHttpResponse create(StrategyDetailHttpResponse draft) {
        return new StrategyDetailHttpResponse(
                draft.status(),
                draft.strategyId(),
                draft.name(),
                draft.description(),
                draft.privacy(),
                draft.versionNumber(),
                draft.rules(),
                draft.pnl(),
                draft.drawdown());
    }
}
