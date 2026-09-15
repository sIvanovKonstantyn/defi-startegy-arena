package com.defistrategyarena.strategy.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final String OWNER_ID = "user-42";
    private static final String STRATEGY_NAME = "rsi-bounce";
    private static final String RULE_ID = "r1";
    private static final String RULE_TYPE_PRICE_ABOVE = "price_above";
    private static final String RULE_TYPE_UNKNOWN = "moon_phase";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String THRESHOLD = "3000";
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
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID, RULE_TYPE_PRICE_ABOVE, INSTRUMENT, THRESHOLD)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_CREATED, response.status());
        assertNotEquals(EMPTY_ID, response.strategyId());
        StrategyId id = new StrategyId(response.strategyId());
        Strategy persisted = strategies.get(id).orElseThrow();
        assertEquals(OWNER_ID, persisted.ownerId());
        assertEquals(STRATEGY_NAME, persisted.current().definition().name());
        assertEquals(Privacy.PRIVATE, persisted.privacy());
        assertEquals(INITIAL_VERSION, persisted.current().number());
        List<StrategyVersionPublished> published = publishedVersions();
        assertEquals(SINGLE_STRATEGY, published.size());
        assertEquals(response.strategyId(), published.getFirst().strategyId());
        assertEquals(OWNER_ID, published.getFirst().ownerId());
        assertEquals(INITIAL_VERSION, published.getFirst().versionNumber());
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
    void rejects_unknown_rule_type() {
        CreateStrategyHttpRequest request =
                new CreateStrategyHttpRequest(
                        OWNER_ID,
                        STRATEGY_NAME,
                        List.of(
                                new CreateStrategyHttpRequest.RuleBody(
                                        RULE_ID, RULE_TYPE_UNKNOWN, INSTRUMENT, THRESHOLD)));

        CreateStrategyHttpResponse response = http.create(request);

        assertEquals(STATUS_BAD_REQUEST, response.status());
        assertEquals(EMPTY_STORE, strategies.size());
        assertTrue(publishedVersions().isEmpty());
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
