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
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;
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
    private static final int SECOND_RULE_INDEX = 1;
    private static final String OWNER_ID = "user-42";
    private static final String STRATEGY_NAME = "rsi-bounce";
    private static final String RULE_ID = "r1";
    private static final String RULE_ID_TWO = "r2";
    private static final String CONDITION_PRICE_ABOVE = "price_above";
    private static final String CONDITION_PRICE_UNDER = "price_under";
    private static final String CONDITION_INDICATOR_BELOW = "indicator_below";
    private static final String CONDITION_INDICATOR_ABOVE = "indicator_above";
    private static final String CONDITION_UNKNOWN = "moon_phase";
    private static final String ACTION_HOLD = "hold";
    private static final String ACTION_BUY = "buy";
    private static final String ACTION_SELL = "sell";
    private static final String ACTION_UNKNOWN = "yeet";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String INDICATOR = "rsi_14";
    private static final String THRESHOLD = "3000";
    private static final String INDICATOR_THRESHOLD = "30";
    private static final String ALLOCATION = "10";
    private static final String EMPTY = "";
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
        http = new StrategyRestAdapter(new StrategyRestAdapter.StrategyRestAdapterDeps(useCase));
    }

    @Test
    void creates_private_strategy_via_rest_adapter_and_emits_version_published() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(priceAboveHold(RULE_ID)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_CREATED, response.status());
        assertNotEquals(EMPTY_ID, response.strategyId());
        StrategyId id = new StrategyId(response.strategyId());
        Strategy persisted = strategies.get(id).orElseThrow();
        assertEquals(OWNER_ID, persisted.ownerId());
        assertEquals(STRATEGY_NAME, persisted.current().definition().name());
        assertEquals(Privacy.PRIVATE, persisted.privacy());
        assertEquals(INITIAL_VERSION, persisted.current().number());
        StrategyDefinition.Rule rule = persisted.current().definition().rules().getFirst();
        assertInstanceOf(StrategyDefinition.PriceAbove.class, rule.when());
        assertInstanceOf(StrategyDefinition.Hold.class, rule.then());
        List<StrategyVersionPublished> published = publishedVersions();
        assertEquals(SINGLE_STRATEGY, published.size());
        assertEquals(response.strategyId(), published.getFirst().strategyId());
        assertEquals(OWNER_ID, published.getFirst().ownerId());
        assertEquals(INITIAL_VERSION, published.getFirst().versionNumber());
    }

    @Test
    void creates_with_price_under_and_sell() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_PRICE_UNDER,
                                        ACTION_SELL,
                                        INSTRUMENT,
                                        EMPTY,
                                        THRESHOLD,
                                        ALLOCATION)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_CREATED, response.status());
        StrategyDefinition.Rule rule =
                strategies
                        .get(new StrategyId(response.strategyId()))
                        .orElseThrow()
                        .current()
                        .definition()
                        .rules()
                        .getFirst();
        assertInstanceOf(StrategyDefinition.PriceUnder.class, rule.when());
        StrategyDefinition.Sell sell = assertInstanceOf(StrategyDefinition.Sell.class, rule.then());
        assertEquals(INSTRUMENT, sell.instrument());
        assertEquals(ALLOCATION, sell.allocationPercent());
    }

    @Test
    void creates_with_indicator_conditions_hold_and_buy() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_INDICATOR_BELOW,
                                        ACTION_HOLD,
                                        EMPTY,
                                        INDICATOR,
                                        INDICATOR_THRESHOLD,
                                        EMPTY),
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID_TWO,
                                        CONDITION_INDICATOR_ABOVE,
                                        ACTION_BUY,
                                        INSTRUMENT,
                                        INDICATOR,
                                        INDICATOR_THRESHOLD,
                                        ALLOCATION)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_CREATED, response.status());
        List<StrategyDefinition.Rule> rules =
                strategies
                        .get(new StrategyId(response.strategyId()))
                        .orElseThrow()
                        .current()
                        .definition()
                        .rules();
        assertEquals(TWO_RULES, rules.size());
        assertInstanceOf(StrategyDefinition.IndicatorBelow.class, rules.getFirst().when());
        assertInstanceOf(StrategyDefinition.Hold.class, rules.getFirst().then());
        assertInstanceOf(StrategyDefinition.IndicatorAbove.class, rules.get(SECOND_RULE_INDEX).when());
        assertInstanceOf(StrategyDefinition.Buy.class, rules.get(SECOND_RULE_INDEX).then());
    }

    @Test
    void rejects_duplicate_owner_and_name() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(OWNER_ID, STRATEGY_NAME, List.of());

        CreateStrategyHttpResponse first = http.create(request);
        CreateStrategyHttpResponse second = http.create(request);

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
                http.create(new CreateStrategyHttpRequest(" ", STRATEGY_NAME, List.of()));

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_blank_strategy_name() {
        CreateStrategyHttpResponse response =
                http.create(new CreateStrategyHttpRequest(OWNER_ID, " ", List.of()));

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_unknown_condition_type() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_UNKNOWN,
                                        ACTION_HOLD,
                                        INSTRUMENT,
                                        EMPTY,
                                        THRESHOLD,
                                        EMPTY)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_unknown_action_type() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_PRICE_ABOVE,
                                        ACTION_UNKNOWN,
                                        INSTRUMENT,
                                        EMPTY,
                                        THRESHOLD,
                                        EMPTY)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_blank_allocation_on_buy() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_PRICE_ABOVE,
                                        ACTION_BUY,
                                        INSTRUMENT,
                                        EMPTY,
                                        THRESHOLD,
                                        EMPTY)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_blank_instrument_on_sell() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        CONDITION_PRICE_UNDER,
                                        ACTION_SELL,
                                        EMPTY,
                                        EMPTY,
                                        THRESHOLD,
                                        ALLOCATION)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    @Test
    void rejects_null_condition_type_as_unknown() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID,
                                        null,
                                        ACTION_HOLD,
                                        INSTRUMENT,
                                        EMPTY,
                                        THRESHOLD,
                                        EMPTY)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
    }

    private static CreateStrategyHttpRequest.RuleBody priceAboveHold(String ruleId) {
        return new CreateStrategyHttpRequest.RuleBody(
                ruleId,
                CONDITION_PRICE_ABOVE,
                ACTION_HOLD,
                INSTRUMENT,
                EMPTY,
                THRESHOLD,
                EMPTY);
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
