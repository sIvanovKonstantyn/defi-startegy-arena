package com.defistrategyarena.strategy.unit.adapter.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.CreateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyFailed;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyRequested;
import com.defistrategyarena.shared.events.strategy.GetStrategyFailed;
import com.defistrategyarena.shared.events.strategy.GetStrategyRequested;
import com.defistrategyarena.shared.events.strategy.ListStrategiesFailed;
import com.defistrategyarena.shared.events.strategy.ListStrategiesRequested;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrategyRequestListenersTest {

    private static final String CORRELATION = "c-1";
    private static final String OWNER = "owner-1";
    private static final String BLANK = " ";
    private static final String REASON_BAD_REQUEST = "BAD_REQUEST";
    private static final int PAGE = 1;
    private static final int SIZE = 20;
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;

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
        registry.dispatch(new CreateStrategyRequested(CORRELATION, OWNER, BLANK, List.of()));
        assertEquals(SINGLE, events.published().size());
        CreateStrategyFailed failed = assertInstanceOf(CreateStrategyFailed.class, events.published().get(EMPTY));
        assertEquals(REASON_BAD_REQUEST, failed.reasonCode());
    }

    @Test
    void list_with_invalid_sort_publishes_failed() {
        registry.dispatch(
                new ListStrategiesRequested(
                        CORRELATION,
                        OWNER,
                        PAGE,
                        SIZE,
                        "not-a-sort",
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
