package com.defistrategyarena.strategy.unit.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.CreateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.CreateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyFailed;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyRequested;
import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.GetStrategyFailed;
import com.defistrategyarena.shared.events.strategy.GetStrategyRequested;
import com.defistrategyarena.shared.events.strategy.ListStrategiesCompleted;
import com.defistrategyarena.shared.events.strategy.ListStrategiesFailed;
import com.defistrategyarena.shared.events.strategy.ListStrategiesRequested;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyRequested;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.adapter.messaging.StrategyRequestListeners;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrategyRequestListenersTest {

    private static final String CORRELATION = "c-1";
    private static final String OWNER = "owner-1";
    private static final String NAME = "alpha";
    private static final String DESCRIPTION = StrategyTestFixtures.DESCRIPTION;
    private static final String RULE_ID = "r1";
    private static final String BLANK = " ";
    private static final String UNKNOWN_INDICATOR = "moon_phase";
    private static final String UNKNOWN_STRATEGY_ID = "00000000-0000-0000-0000-000000000099";
    private static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    private static final String REASON_NOT_FOUND = "NOT_FOUND";
    private static final String REASON_DUPLICATE = "DUPLICATE";
    private static final String SORT_UNKNOWN = "not-a-sort";
    private static final int PAGE = 0;
    private static final int SIZE = 20;
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
    private static final int TWO_CHILDREN = 2;
    private static final int VERSION_TWO = 2;

    private DomainEventListenerRegistry registry;
    private RecordingPublisher events;

    @BeforeEach
    void setUp() {
        InMemoryStrategyRepository strategies = new InMemoryStrategyRepository();
        events = new RecordingPublisher();
        StrategyUseCases useCases =
                new StrategyUseCases(
                        new CreateStrategy(new CreateStrategy.CreateStrategyDeps(strategies, events)),
                        new ListStrategies(new ListStrategies.ListStrategiesDeps(strategies)),
                        new GetStrategy(new GetStrategy.GetStrategyDeps(strategies)),
                        new UpdateStrategy(
                                new UpdateStrategy.UpdateStrategyDeps(strategies, events)),
                        new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(strategies)));
        registry = new DomainEventListenerRegistry();
        new StrategyRequestListeners(
                        new StrategyRequestListeners.StrategyRequestListenersDeps(useCases, events))
                .register(registry);
    }

    @Test
    void create_with_blank_name_publishes_failed() {
        registry.dispatch(
                new CreateStrategyRequested(CORRELATION, OWNER, BLANK, DESCRIPTION, List.of()));
        assertEquals(SINGLE, events.published().size());
        CreateStrategyFailed failed = assertInstanceOf(CreateStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void create_with_unknown_indicator_publishes_failed() {
        registry.dispatch(
                new CreateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        NAME,
                        DESCRIPTION,
                        List.of(
                                StrategyTestFixtures.rule(
                                                RULE_ID,
                                                StrategyTestFixtures.indicatorCompare(
                                                        UNKNOWN_INDICATOR,
                                                        StrategyTestFixtures.OPERATOR_LT,
                                                        StrategyTestFixtures.THRESHOLD,
                                                        StrategyTestFixtures.PERIOD_PARAMS),
                                                StrategyTestFixtures.hold())
                                        .toEvent())));
        assertEquals(SINGLE, events.published().size());
        CreateStrategyFailed failed =
                assertInstanceOf(CreateStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void create_then_get_and_update_round_trip_condition_trees() {
        registry.dispatch(
                new CreateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        NAME,
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.andOpenLpBody(RULE_ID).toEvent())));
        CreateStrategyCompleted created = lastOfType(CreateStrategyCompleted.class);

        registry.dispatch(new GetStrategyRequested(CORRELATION, OWNER, created.strategyId()));
        GetStrategyCompleted detail = lastOfType(GetStrategyCompleted.class);
        assertEquals(DESCRIPTION, detail.description());
        CreateStrategyRequested.RulePayload rule = detail.rules().getFirst();
        assertEquals(StrategyTestFixtures.CONDITION_AND, rule.when().type());
        assertEquals(TWO_CHILDREN, rule.when().children().size());
        assertEquals(StrategyTestFixtures.ACTION_OPEN_LP, rule.then().type());
        assertEquals(StrategyTestFixtures.INSTRUMENT_PAIR, rule.then().instrumentPair());

        registry.dispatch(
                new UpdateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        created.strategyId(),
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.priceLtHoldBody(RULE_ID).toEvent())));
        UpdateStrategyCompleted updated = lastOfType(UpdateStrategyCompleted.class);
        assertEquals(VERSION_TWO, updated.versionNumber());

        registry.dispatch(
                new ListStrategiesRequested(
                        CORRELATION, OWNER, PAGE, SIZE, ListStrategiesQuery.SORT_NAME, ListStrategiesQuery.ORDER_ASC));
        ListStrategiesCompleted listed = lastOfType(ListStrategiesCompleted.class);
        assertEquals(DESCRIPTION, listed.items().getFirst().description());
    }

    @Test
    void update_with_unknown_strategy_publishes_failed() {
        registry.dispatch(
                new UpdateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        UNKNOWN_STRATEGY_ID,
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.priceGtHoldBody(RULE_ID).toEvent())));
        assertEquals(SINGLE, events.published().size());
        UpdateStrategyFailed failed =
                assertInstanceOf(UpdateStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_NOT_FOUND, failed.reasonCode());
    }

    @Test
    void update_with_blank_strategy_id_publishes_failed() {
        registry.dispatch(
                new UpdateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        BLANK,
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.priceGtHoldBody(RULE_ID).toEvent())));
        assertEquals(SINGLE, events.published().size());
        UpdateStrategyFailed failed =
                assertInstanceOf(UpdateStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void get_with_unknown_strategy_publishes_not_found() {
        registry.dispatch(new GetStrategyRequested(CORRELATION, OWNER, UNKNOWN_STRATEGY_ID));
        assertEquals(SINGLE, events.published().size());
        GetStrategyFailed failed =
                assertInstanceOf(GetStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_NOT_FOUND, failed.reasonCode());
    }

    @Test
    void create_duplicate_name_publishes_duplicate() {
        CreateStrategyRequested request =
                new CreateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        NAME,
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.priceGtHoldBody(RULE_ID).toEvent()));
        registry.dispatch(request);
        registry.dispatch(request);
        CreateStrategyFailed failed =
                assertInstanceOf(CreateStrategyFailed.class, events.published().getLast());
        assertEquals(REASON_DUPLICATE, failed.reasonCode());
    }

    @Test
    void delete_unknown_and_known_strategy_publish_expected_events() {
        registry.dispatch(new DeleteStrategyRequested(CORRELATION, OWNER, UNKNOWN_STRATEGY_ID));
        DeleteStrategyFailed notFound =
                assertInstanceOf(DeleteStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_NOT_FOUND, notFound.reasonCode());

        registry.dispatch(
                new CreateStrategyRequested(
                        CORRELATION,
                        OWNER,
                        NAME,
                        DESCRIPTION,
                        List.of(StrategyTestFixtures.priceGtHoldBody(RULE_ID).toEvent())));
        CreateStrategyCompleted created = lastOfType(CreateStrategyCompleted.class);
        registry.dispatch(new DeleteStrategyRequested(CORRELATION, OWNER, created.strategyId()));
        DeleteStrategyCompleted deleted =
                assertInstanceOf(DeleteStrategyCompleted.class, events.published().getLast());
        assertEquals(created.strategyId(), deleted.strategyId());
    }

    @Test
    void list_with_invalid_sort_publishes_failed() {
        registry.dispatch(
                new ListStrategiesRequested(
                        CORRELATION,
                        OWNER,
                        PAGE,
                        SIZE,
                        SORT_UNKNOWN,
                        ListStrategiesQuery.ORDER_ASC));
        assertEquals(SINGLE, events.published().size());
        ListStrategiesFailed failed =
                assertInstanceOf(ListStrategiesFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void get_with_blank_strategy_id_publishes_failed() {
        registry.dispatch(new GetStrategyRequested(CORRELATION, OWNER, BLANK));
        assertEquals(SINGLE, events.published().size());
        GetStrategyFailed failed = assertInstanceOf(GetStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void delete_with_blank_strategy_id_publishes_failed() {
        registry.dispatch(new DeleteStrategyRequested(CORRELATION, OWNER, BLANK));
        assertEquals(SINGLE, events.published().size());
        DeleteStrategyFailed failed =
                assertInstanceOf(DeleteStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
        assertTrue(failed.correlationId().equals(CORRELATION));
    }

    private <T extends DomainEvent> T lastOfType(Class<T> type) {
        for (int index = events.published().size() - SINGLE; index >= EMPTY; index--) {
            DomainEvent event = events.published().get(index);
            if (type.isInstance(event)) {
                return type.cast(event);
            }
        }
        throw new AssertionError("missing event of type " + type.getName());
    }

    private static final class RecordingPublisher implements DomainEventPublisher {
        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }

        List<DomainEvent> published() {
            return List.copyOf(published);
        }
    }
}
