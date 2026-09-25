package com.defistrategyarena.strategy.testsupport;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.indicators.IndicatorId;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpRequest;
import com.defistrategyarena.strategy.domain.CompareOperator;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import java.util.List;
import java.util.Map;

public final class StrategyTestFixtures {

    public static final String INSTRUMENT = "ETH-USD";
    public static final String INSTRUMENT_PAIR = "ETH-USDC";
    public static final String THRESHOLD = "3000";
    public static final String THRESHOLD_LOW = "2500";
    public static final String ALLOCATION = "10";
    public static final String YEARLY_FEE = "0.3";
    public static final String EMPTY = "";
    public static final String DESCRIPTION = "test strategy";
    public static final String INDICATOR_SMA = "sma";
    public static final String INDICATOR_RSI = "rsi";
    public static final String PERIOD = "period";
    public static final String PERIOD_VALUE = "14";
    public static final String OPERATOR_GT = "gt";
    public static final String OPERATOR_LT = "lt";
    public static final String CONDITION_AND = "and";
    public static final String CONDITION_OR = "or";
    public static final String CONDITION_PRICE_COMPARE = "price_compare";
    public static final String CONDITION_INDICATOR_COMPARE = "indicator_compare";
    public static final String ACTION_HOLD = "hold";
    public static final String ACTION_BUY = "buy";
    public static final String ACTION_SELL = "sell";
    public static final String ACTION_OPEN_LP = "open_lp";
    public static final Map<String, String> PERIOD_PARAMS = Map.of(PERIOD, PERIOD_VALUE);

    private StrategyTestFixtures() {}

    public static StrategyDefinition.Rule priceGtHold(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.PriceCompare(INSTRUMENT, CompareOperator.GT, THRESHOLD),
                new StrategyDefinition.Hold());
    }

    public static StrategyDefinition.Rule priceLtHold(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.PriceCompare(INSTRUMENT, CompareOperator.LT, THRESHOLD_LOW),
                new StrategyDefinition.Hold());
    }

    public static StrategyDefinition.Rule priceGtBuy(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.PriceCompare(INSTRUMENT, CompareOperator.GT, THRESHOLD),
                new StrategyDefinition.Buy(INSTRUMENT, ALLOCATION));
    }

    public static StrategyDefinition.Rule indicatorLtSell(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.IndicatorCompare(
                        new IndicatorId(INDICATOR_RSI),
                        PERIOD_PARAMS,
                        CompareOperator.LT,
                        THRESHOLD_LOW),
                new StrategyDefinition.Sell(INSTRUMENT, ALLOCATION));
    }

    public static StrategyDefinition.Rule andOpenLp(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.And(
                        List.of(
                                new StrategyDefinition.IndicatorCompare(
                                        new IndicatorId(INDICATOR_SMA),
                                        PERIOD_PARAMS,
                                        CompareOperator.LT,
                                        THRESHOLD),
                                new StrategyDefinition.PriceCompare(
                                        INSTRUMENT, CompareOperator.GT, THRESHOLD_LOW))),
                new StrategyDefinition.OpenLp(INSTRUMENT_PAIR, ALLOCATION, YEARLY_FEE));
    }

    public static StrategyDefinition.Rule orHold(String ruleId) {
        return new StrategyDefinition.Rule(
                ruleId,
                new StrategyDefinition.Or(
                        List.of(
                                new StrategyDefinition.PriceCompare(
                                        INSTRUMENT, CompareOperator.GTE, THRESHOLD),
                                new StrategyDefinition.PriceCompare(
                                        INSTRUMENT, CompareOperator.LTE, THRESHOLD_LOW))),
                new StrategyDefinition.Hold());
    }

    public static StrategyDefinition definition(String name, StrategyDefinition.Rule rule) {
        return StrategyDefinition.create(new StrategyDefinition(name, DESCRIPTION, List.of(rule)));
    }

    public static StrategyDefinition emptyDefinition(String name) {
        return StrategyDefinition.create(new StrategyDefinition(name, DESCRIPTION, List.of()));
    }

    public static CreateStrategyHttpRequest.ConditionBody priceCompare(
            String operator, String threshold) {
        return new CreateStrategyHttpRequest.ConditionBody(
                CONDITION_PRICE_COMPARE,
                List.of(),
                INSTRUMENT,
                EMPTY,
                operator,
                threshold,
                Map.of());
    }

    public static CreateStrategyHttpRequest.ConditionBody indicatorCompare(
            String indicator, String operator, String threshold, Map<String, String> parameters) {
        return new CreateStrategyHttpRequest.ConditionBody(
                CONDITION_INDICATOR_COMPARE,
                List.of(),
                EMPTY,
                indicator,
                operator,
                threshold,
                parameters);
    }

    public static CreateStrategyHttpRequest.ConditionBody group(
            String type, List<CreateStrategyHttpRequest.ConditionBody> children) {
        return new CreateStrategyHttpRequest.ConditionBody(
                type, children, EMPTY, EMPTY, EMPTY, EMPTY, Map.of());
    }

    public static CreateStrategyHttpRequest.ConditionBody smaLtAndPriceGt() {
        return group(
                CONDITION_AND,
                List.of(
                        indicatorCompare(INDICATOR_SMA, OPERATOR_LT, THRESHOLD, PERIOD_PARAMS),
                        priceCompare(OPERATOR_GT, THRESHOLD_LOW)));
    }

    public static CreateStrategyHttpRequest.ActionBody hold() {
        return new CreateStrategyHttpRequest.ActionBody(ACTION_HOLD, EMPTY, EMPTY, EMPTY, EMPTY);
    }

    public static CreateStrategyHttpRequest.ActionBody buy(String allocationPercent) {
        return new CreateStrategyHttpRequest.ActionBody(
                ACTION_BUY, INSTRUMENT, EMPTY, allocationPercent, EMPTY);
    }

    public static CreateStrategyHttpRequest.ActionBody sell(String allocationPercent) {
        return new CreateStrategyHttpRequest.ActionBody(
                ACTION_SELL, INSTRUMENT, EMPTY, allocationPercent, EMPTY);
    }

    public static CreateStrategyHttpRequest.ActionBody openLp(
            String pair, String allocation, String yearlyFee) {
        return new CreateStrategyHttpRequest.ActionBody(
                ACTION_OPEN_LP, EMPTY, pair, allocation, yearlyFee);
    }

    public static CreateStrategyHttpRequest.RuleBody rule(
            String id,
            CreateStrategyHttpRequest.ConditionBody when,
            CreateStrategyHttpRequest.ActionBody then) {
        return new CreateStrategyHttpRequest.RuleBody(id, when, then);
    }

    public static CreateStrategyHttpRequest.RuleBody priceGtHoldBody(String id) {
        return rule(id, priceCompare(OPERATOR_GT, THRESHOLD), hold());
    }

    public static CreateStrategyHttpRequest.RuleBody priceLtHoldBody(String id) {
        return rule(id, priceCompare(OPERATOR_LT, THRESHOLD_LOW), hold());
    }

    public static CreateStrategyHttpRequest.RuleBody andOpenLpBody(String id) {
        return rule(id, smaLtAndPriceGt(), openLp(INSTRUMENT_PAIR, ALLOCATION, YEARLY_FEE));
    }

    public static CreateStrategyHttpRequest createRequest(
            String name, CreateStrategyHttpRequest.RuleBody rule) {
        return new CreateStrategyHttpRequest(name, DESCRIPTION, List.of(rule));
    }

    public static CreateStrategyHttpRequest createRequest(
            String name, List<CreateStrategyHttpRequest.RuleBody> rules) {
        return new CreateStrategyHttpRequest(name, DESCRIPTION, rules);
    }

    public static UpdateStrategyHttpRequest updateRequest(
            List<CreateStrategyHttpRequest.RuleBody> rules) {
        return new UpdateStrategyHttpRequest(DESCRIPTION, rules);
    }

    public static CreateStrategyRequested.RulePayload priceGtHoldPayload(String id) {
        return priceGtHoldBody(id).toEvent();
    }
}
