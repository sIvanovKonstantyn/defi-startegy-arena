package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.CreateStrategyCommand;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;

public final class StrategyRestAdapter {

    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_CONFLICT = 409;
    private static final String EMPTY_STRATEGY_ID = "";
    private static final String RULE_TYPE_PRICE_ABOVE = "price_above";
    private static final String UNKNOWN_RULE_TYPE = "unknown rule type";

    private final CreateStrategy createStrategy;

    public StrategyRestAdapter(StrategyRestAdapterDeps deps) {
        this.createStrategy = deps.createStrategy();
    }

    public CreateStrategyHttpResponse create(CreateStrategyHttpRequest request) {
        try {
            StrategyId id =
                    createStrategy.execute(
                            CreateStrategyCommand.create(
                                    new CreateStrategyCommand(request.ownerId(), toDefinition(request))));
            return new CreateStrategyHttpResponse(STATUS_CREATED, id.value());
        } catch (DuplicateStrategyException exception) {
            return new CreateStrategyHttpResponse(STATUS_CONFLICT, EMPTY_STRATEGY_ID);
        } catch (IllegalArgumentException exception) {
            return new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY_STRATEGY_ID);
        }
    }

    private static StrategyDefinition toDefinition(CreateStrategyHttpRequest request) {
        List<StrategyDefinition.Rule> rules = new ArrayList<>();
        for (CreateStrategyHttpRequest.RuleBody body : request.rules()) {
            rules.add(toRule(body));
        }
        return StrategyDefinition.create(new StrategyDefinition(request.name(), rules));
    }

    private static StrategyDefinition.Rule toRule(CreateStrategyHttpRequest.RuleBody body) {
        if (!RULE_TYPE_PRICE_ABOVE.equals(body.type())) {
            throw new IllegalArgumentException(UNKNOWN_RULE_TYPE);
        }
        return new StrategyDefinition.Rule(
                body.id(),
                new StrategyDefinition.PriceAbove(body.instrument(), body.threshold()),
                new StrategyDefinition.Hold());
    }

    public record StrategyRestAdapterDeps(CreateStrategy createStrategy) {}
}
