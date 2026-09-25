package com.defistrategyarena.strategy.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.domain.CompareOperator;
import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateStrategyE2ETest {

    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_CONFLICT = 409;
    private static final int INITIAL_VERSION = 1;
    private static final int EMPTY_STORE = 0;
    private static final int SINGLE_STRATEGY = 1;
    private static final int TWO_RULES = 2;
    private static final int TWO_CHILDREN = 2;
    private static final int SECOND_INDEX = 1;
    private static final String OWNER_ID = "user-42";
    private static final String STRATEGY_NAME = "rsi-bounce";
    private static final String RULE_ID = "r1";
    private static final String RULE_ID_TWO = "r2";
    private static final String CONDITION_UNKNOWN = "moon_phase";
    private static final String ACTION_UNKNOWN = "yeet";
    private static final String INDICATOR_UNKNOWN = "moon_phase";
    private static final String OPERATOR_UNKNOWN = "approximately";
    private static final String BLANK = " ";
    private static final String EMPTY_ID = "";

    private InMemoryStrategyRepository strategies;
    private RecordingEventPublisher events;
    private StrategyRestAdapter http;

    @BeforeEach
    void setUp() {
        strategies = new InMemoryStrategyRepository();
        events = new RecordingEventPublisher();
        CreateStrategy useCase =
                new CreateStrategy(new CreateStrategy.CreateStrategyDeps(strategies, events));
        ListStrategies listStrategies =
                new ListStrategies(new ListStrategies.ListStrategiesDeps(strategies));
        GetStrategy getStrategy = new GetStrategy(new GetStrategy.GetStrategyDeps(strategies));
        UpdateStrategy updateStrategy =
                new UpdateStrategy(new UpdateStrategy.UpdateStrategyDeps(strategies, events));
        DeleteStrategy deleteStrategy =
                new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(strategies));
        http =
                new StrategyRestAdapter(
                        new StrategyRestAdapter.StrategyRestAdapterDeps(
                                new StrategyUseCases(
                                        useCase,
                                        listStrategies,
                                        getStrategy,
                                        updateStrategy,
                                        deleteStrategy)));
    }

    @Test
    void creates_private_strategy_via_rest_adapter_and_emits_version_published() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME, StrategyTestFixtures.priceGtHoldBody(RULE_ID)));

        assertEquals(STATUS_CREATED, response.status());
        assertNotEquals(EMPTY_ID, response.strategyId());
        Strategy persisted = strategies.get(new StrategyId(response.strategyId())).orElseThrow();
        assertEquals(OWNER_ID, persisted.ownerId());
        assertEquals(STRATEGY_NAME, persisted.current().definition().name());
        assertEquals(StrategyTestFixtures.DESCRIPTION, persisted.current().definition().description());
        assertEquals(Privacy.PRIVATE, persisted.privacy());
        assertEquals(INITIAL_VERSION, persisted.current().number());
        StrategyDefinition.Rule rule = persisted.current().definition().rules().getFirst();
        StrategyDefinition.PriceCompare when =
                assertInstanceOf(StrategyDefinition.PriceCompare.class, rule.when());
        assertEquals(CompareOperator.GT, when.operator());
        assertEquals(StrategyTestFixtures.THRESHOLD, when.threshold());
        assertInstanceOf(StrategyDefinition.Hold.class, rule.then());
        List<StrategyVersionPublished> published = publishedVersions();
        assertEquals(SINGLE_STRATEGY, published.size());
        assertEquals(response.strategyId(), published.getFirst().strategyId());
        assertEquals(OWNER_ID, published.getFirst().ownerId());
        assertEquals(INITIAL_VERSION, published.getFirst().versionNumber());
    }

    @Test
    void creates_with_price_compare_lt_and_sell() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.priceCompare(
                                                StrategyTestFixtures.OPERATOR_LT,
                                                StrategyTestFixtures.THRESHOLD_LOW),
                                        StrategyTestFixtures.sell(StrategyTestFixtures.ALLOCATION))));

        assertEquals(STATUS_CREATED, response.status());
        StrategyDefinition.Rule rule = firstRule(response);
        StrategyDefinition.PriceCompare when =
                assertInstanceOf(StrategyDefinition.PriceCompare.class, rule.when());
        assertEquals(CompareOperator.LT, when.operator());
        StrategyDefinition.Sell sell = assertInstanceOf(StrategyDefinition.Sell.class, rule.then());
        assertEquals(StrategyTestFixtures.INSTRUMENT, sell.instrument());
        assertEquals(StrategyTestFixtures.ALLOCATION, sell.allocationPercent());
    }

    @Test
    void creates_with_indicator_conditions_hold_and_buy() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                List.of(
                                        StrategyTestFixtures.rule(
                                                RULE_ID,
                                                StrategyTestFixtures.indicatorCompare(
                                                        StrategyTestFixtures.INDICATOR_RSI,
                                                        StrategyTestFixtures.OPERATOR_LT,
                                                        StrategyTestFixtures.THRESHOLD_LOW,
                                                        StrategyTestFixtures.PERIOD_PARAMS),
                                                StrategyTestFixtures.hold()),
                                        StrategyTestFixtures.rule(
                                                RULE_ID_TWO,
                                                StrategyTestFixtures.indicatorCompare(
                                                        StrategyTestFixtures.INDICATOR_SMA,
                                                        StrategyTestFixtures.OPERATOR_GT,
                                                        StrategyTestFixtures.THRESHOLD,
                                                        StrategyTestFixtures.PERIOD_PARAMS),
                                                StrategyTestFixtures.buy(
                                                        StrategyTestFixtures.ALLOCATION)))));

        assertEquals(STATUS_CREATED, response.status());
        List<StrategyDefinition.Rule> rules = rules(response);
        assertEquals(TWO_RULES, rules.size());
        StrategyDefinition.IndicatorCompare first =
                assertInstanceOf(StrategyDefinition.IndicatorCompare.class, rules.getFirst().when());
        assertEquals(StrategyTestFixtures.INDICATOR_RSI, first.indicatorId().value());
        assertEquals(StrategyTestFixtures.PERIOD_PARAMS, first.parameters());
        assertInstanceOf(StrategyDefinition.Hold.class, rules.getFirst().then());
        StrategyDefinition.IndicatorCompare second =
                assertInstanceOf(
                        StrategyDefinition.IndicatorCompare.class,
                        rules.get(SECOND_INDEX).when());
        assertEquals(StrategyTestFixtures.INDICATOR_SMA, second.indicatorId().value());
        assertEquals(CompareOperator.GT, second.operator());
        assertInstanceOf(StrategyDefinition.Buy.class, rules.get(SECOND_INDEX).then());
    }

    @Test
    void creates_and_condition_tree_with_open_lp_action() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME, StrategyTestFixtures.andOpenLpBody(RULE_ID)));

        assertEquals(STATUS_CREATED, response.status());
        StrategyDefinition.Rule rule = firstRule(response);
        StrategyDefinition.And when =
                assertInstanceOf(StrategyDefinition.And.class, rule.when());
        assertEquals(TWO_CHILDREN, when.children().size());
        StrategyDefinition.IndicatorCompare sma =
                assertInstanceOf(
                        StrategyDefinition.IndicatorCompare.class, when.children().getFirst());
        assertEquals(StrategyTestFixtures.INDICATOR_SMA, sma.indicatorId().value());
        assertEquals(CompareOperator.LT, sma.operator());
        assertEquals(StrategyTestFixtures.THRESHOLD, sma.threshold());
        StrategyDefinition.PriceCompare price =
                assertInstanceOf(
                        StrategyDefinition.PriceCompare.class, when.children().get(SECOND_INDEX));
        assertEquals(CompareOperator.GT, price.operator());
        assertEquals(StrategyTestFixtures.THRESHOLD_LOW, price.threshold());
        StrategyDefinition.OpenLp openLp =
                assertInstanceOf(StrategyDefinition.OpenLp.class, rule.then());
        assertEquals(StrategyTestFixtures.INSTRUMENT_PAIR, openLp.instrumentPair());
        assertEquals(StrategyTestFixtures.ALLOCATION, openLp.allocationPercent());
        assertEquals(StrategyTestFixtures.YEARLY_FEE, openLp.yearlyFeePercent());
        assertEquals(SINGLE_STRATEGY, strategies.size());
        assertEquals(SINGLE_STRATEGY, publishedVersions().size());
    }

    @Test
    void creates_or_condition_tree() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.group(
                                                StrategyTestFixtures.CONDITION_OR,
                                                List.of(
                                                        StrategyTestFixtures.priceCompare(
                                                                StrategyTestFixtures.OPERATOR_GT,
                                                                StrategyTestFixtures.THRESHOLD),
                                                        StrategyTestFixtures.priceCompare(
                                                                StrategyTestFixtures.OPERATOR_LT,
                                                                StrategyTestFixtures.THRESHOLD_LOW))),
                                        StrategyTestFixtures.hold())));

        assertEquals(STATUS_CREATED, response.status());
        StrategyDefinition.Or when =
                assertInstanceOf(StrategyDefinition.Or.class, firstRule(response).when());
        assertEquals(TWO_CHILDREN, when.children().size());
    }

    @Test
    void rejects_unknown_indicator() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.indicatorCompare(
                                                INDICATOR_UNKNOWN,
                                                StrategyTestFixtures.OPERATOR_LT,
                                                StrategyTestFixtures.THRESHOLD,
                                                StrategyTestFixtures.PERIOD_PARAMS),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_indicator_with_missing_required_parameter() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.indicatorCompare(
                                                StrategyTestFixtures.INDICATOR_SMA,
                                                StrategyTestFixtures.OPERATOR_LT,
                                                StrategyTestFixtures.THRESHOLD,
                                                Map.of()),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_empty_and_condition() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.group(
                                                StrategyTestFixtures.CONDITION_AND, List.of()),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_empty_or_condition() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.group(
                                                StrategyTestFixtures.CONDITION_OR, List.of()),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_unknown_compare_operator() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.priceCompare(
                                                OPERATOR_UNKNOWN, StrategyTestFixtures.THRESHOLD),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_duplicate_owner_and_name() {
        CreateStrategyHttpRequest request =
                StrategyTestFixtures.createRequest(STRATEGY_NAME, List.of());

        CreateStrategyHttpResponse first = create(request);
        CreateStrategyHttpResponse second = create(request);

        assertEquals(STATUS_CREATED, first.status());
        assertTrue(strategies.get(new StrategyId(first.strategyId())).isPresent());
        assertEquals(STATUS_CONFLICT, second.status());
        OwnerStrategyName key = new OwnerStrategyName(OWNER_ID, STRATEGY_NAME);
        assertEquals(SINGLE_STRATEGY, strategies.countByOwnerAndName(key));
        assertEquals(
                first.strategyId(),
                strategies.findByOwnerAndName(key).orElseThrow().id().value());
        assertEquals(SINGLE_STRATEGY, publishedVersions().size());
    }

    @Test
    void rejects_blank_owner() {
        CreateStrategyHttpResponse response =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                BLANK,
                                StrategyTestFixtures.createRequest(STRATEGY_NAME, List.of())));

        assertRejected(response);
    }

    @Test
    void rejects_blank_strategy_name() {
        assertRejected(create(StrategyTestFixtures.createRequest(BLANK, List.of())));
    }

    @Test
    void rejects_unknown_condition_type() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.group(CONDITION_UNKNOWN, List.of()),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_null_condition_type_as_unknown() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.group(null, List.of()),
                                        StrategyTestFixtures.hold())));

        assertRejected(response);
    }

    @Test
    void rejects_unknown_action_type() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.priceCompare(
                                                StrategyTestFixtures.OPERATOR_GT,
                                                StrategyTestFixtures.THRESHOLD),
                                        new CreateStrategyHttpRequest.ActionBody(
                                                ACTION_UNKNOWN,
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.EMPTY))));

        assertRejected(response);
    }

    @Test
    void rejects_blank_allocation_on_buy() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.priceCompare(
                                                StrategyTestFixtures.OPERATOR_GT,
                                                StrategyTestFixtures.THRESHOLD),
                                        StrategyTestFixtures.buy(StrategyTestFixtures.EMPTY))));

        assertRejected(response);
    }

    @Test
    void rejects_blank_instrument_on_sell() {
        CreateStrategyHttpResponse response =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.priceCompare(
                                                StrategyTestFixtures.OPERATOR_LT,
                                                StrategyTestFixtures.THRESHOLD_LOW),
                                        new CreateStrategyHttpRequest.ActionBody(
                                                StrategyTestFixtures.ACTION_SELL,
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.ALLOCATION,
                                                StrategyTestFixtures.EMPTY))));

        assertRejected(response);
    }

    @Test
    void rejects_open_lp_with_blank_pair_or_fee() {
        CreateStrategyHttpResponse blankPair =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.smaLtAndPriceGt(),
                                        StrategyTestFixtures.openLp(
                                                StrategyTestFixtures.EMPTY,
                                                StrategyTestFixtures.ALLOCATION,
                                                StrategyTestFixtures.YEARLY_FEE))));
        CreateStrategyHttpResponse blankFee =
                create(
                        StrategyTestFixtures.createRequest(
                                STRATEGY_NAME,
                                StrategyTestFixtures.rule(
                                        RULE_ID,
                                        StrategyTestFixtures.smaLtAndPriceGt(),
                                        StrategyTestFixtures.openLp(
                                                StrategyTestFixtures.INSTRUMENT_PAIR,
                                                StrategyTestFixtures.ALLOCATION,
                                                StrategyTestFixtures.EMPTY))));

        assertRejected(blankPair);
        assertRejected(blankFee);
    }

    private void assertRejected(CreateStrategyHttpResponse response) {
        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    private CreateStrategyHttpResponse create(CreateStrategyHttpRequest request) {
        return http.create(new StrategyRestAdapter.CreateStrategyHttpInput(OWNER_ID, request));
    }

    private StrategyDefinition.Rule firstRule(CreateStrategyHttpResponse response) {
        return rules(response).getFirst();
    }

    private List<StrategyDefinition.Rule> rules(CreateStrategyHttpResponse response) {
        return strategies
                .get(new StrategyId(response.strategyId()))
                .orElseThrow()
                .current()
                .definition()
                .rules();
    }

    private List<StrategyVersionPublished> publishedVersions() {
        return events.published().stream()
                .filter(StrategyVersionPublished.class::isInstance)
                .map(StrategyVersionPublished.class::cast)
                .toList();
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {
        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }

        private List<DomainEvent> published() {
            return List.copyOf(published);
        }
    }
}
