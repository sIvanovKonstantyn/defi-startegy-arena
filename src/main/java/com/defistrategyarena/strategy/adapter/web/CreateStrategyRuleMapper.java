package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

enum CreateStrategyRuleMapper {
    ;

    private static final String UNKNOWN_CONDITION_TYPE = "unknown condition type";
    private static final String UNKNOWN_ACTION_TYPE = "unknown action type";
    private static final String ALLOCATION_REQUIRED = "allocation percent must not be blank";
    private static final String INSTRUMENT_REQUIRED = "instrument must not be blank";

    private static final Map<String, Function<CreateStrategyHttpRequest.RuleBody, StrategyDefinition.Condition>>
            CONDITIONS =
                    Map.of(
                            StrategyDslWireNames.CONDITION_PRICE_ABOVE, CreateStrategyRuleMapper::priceAbove,
                            StrategyDslWireNames.CONDITION_PRICE_UNDER, CreateStrategyRuleMapper::priceUnder,
                            StrategyDslWireNames.CONDITION_INDICATOR_BELOW, CreateStrategyRuleMapper::indicatorBelow,
                            StrategyDslWireNames.CONDITION_INDICATOR_ABOVE, CreateStrategyRuleMapper::indicatorAbove);

    private static final Map<String, Function<CreateStrategyHttpRequest.RuleBody, StrategyDefinition.Action>>
            ACTIONS =
                    Map.of(
                            StrategyDslWireNames.ACTION_HOLD, CreateStrategyRuleMapper::hold,
                            StrategyDslWireNames.ACTION_BUY, CreateStrategyRuleMapper::buy,
                            StrategyDslWireNames.ACTION_SELL, CreateStrategyRuleMapper::sell);

    static StrategyDefinition.Rule toRule(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.Rule(body.id(), toCondition(body), toAction(body));
    }

    private static StrategyDefinition.Condition toCondition(CreateStrategyHttpRequest.RuleBody body) {
        Function<CreateStrategyHttpRequest.RuleBody, StrategyDefinition.Condition> mapper =
                CONDITIONS.get(OptionalText.create(new OptionalText(body.conditionType())).orEmpty());
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_CONDITION_TYPE);
        }
        return mapper.apply(body);
    }

    private static StrategyDefinition.Action toAction(CreateStrategyHttpRequest.RuleBody body) {
        Function<CreateStrategyHttpRequest.RuleBody, StrategyDefinition.Action> mapper =
                ACTIONS.get(OptionalText.create(new OptionalText(body.actionType())).orEmpty());
        if (mapper == null) {
            throw new IllegalArgumentException(UNKNOWN_ACTION_TYPE);
        }
        return mapper.apply(body);
    }

    private static StrategyDefinition.Condition priceAbove(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.PriceAbove(field(new OptionalText(body.instrument())), field(new OptionalText(body.threshold())));
    }

    private static StrategyDefinition.Condition priceUnder(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.PriceUnder(field(new OptionalText(body.instrument())), field(new OptionalText(body.threshold())));
    }

    private static StrategyDefinition.Condition indicatorBelow(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.IndicatorBelow(
                field(new OptionalText(body.indicator())), field(new OptionalText(body.threshold())));
    }

    private static StrategyDefinition.Condition indicatorAbove(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.IndicatorAbove(
                field(new OptionalText(body.indicator())), field(new OptionalText(body.threshold())));
    }

    private static StrategyDefinition.Action hold(CreateStrategyHttpRequest.RuleBody body) {
        Objects.requireNonNull(body);
        return new StrategyDefinition.Hold();
    }

    private static StrategyDefinition.Action buy(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.Buy(requireInstrument(body), requireAllocation(body));
    }

    private static StrategyDefinition.Action sell(CreateStrategyHttpRequest.RuleBody body) {
        return new StrategyDefinition.Sell(requireInstrument(body), requireAllocation(body));
    }

    private static String requireInstrument(CreateStrategyHttpRequest.RuleBody body) {
        String instrument = field(new OptionalText(body.instrument()));
        if (instrument.isBlank()) {
            throw new IllegalArgumentException(INSTRUMENT_REQUIRED);
        }
        return instrument;
    }

    private static String requireAllocation(CreateStrategyHttpRequest.RuleBody body) {
        String allocation = field(new OptionalText(body.allocationPercent()));
        if (allocation.isBlank()) {
            throw new IllegalArgumentException(ALLOCATION_REQUIRED);
        }
        return allocation;
    }

    private static String field(OptionalText text) {
        return text.orEmpty();
    }

    private record OptionalText(String value) {
        private static OptionalText create(OptionalText draft) {
            return draft;
        }

        private String orEmpty() {
            if (value == null) {
                return StrategyDslWireNames.EMPTY;
            }
            return value;
        }
    }
}
