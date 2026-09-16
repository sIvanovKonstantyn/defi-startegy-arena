package com.defistrategyarena.strategy.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.DeleteStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyDetailHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpResponse;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrategyUpdateDeleteE2ETest {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int VERSION_ONE = 1;
    private static final int VERSION_TWO = 2;
    private static final String OWNER = "owner-update";
    private static final String OTHER = "other-owner";
    private static final String NAME = "mutable-rules";
    private static final String EMPTY = "";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String CONDITION_PRICE_ABOVE = "price_above";
    private static final String CONDITION_PRICE_UNDER = "price_under";
    private static final String ACTION_HOLD = "hold";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String THRESHOLD = "3000";
    private static final String THRESHOLD_NEW = "2500";

    private InMemoryStrategyRepository strategies;
    private RecordingEventPublisher events;
    private StrategyRestAdapter http;

    @BeforeEach
    void setUp() {
        strategies = new InMemoryStrategyRepository();
        events = new RecordingEventPublisher();
        CreateStrategy create =
                new CreateStrategy(new CreateStrategy.CreateStrategyDeps(strategies, events));
        ListStrategies list = new ListStrategies(new ListStrategies.ListStrategiesDeps(strategies));
        GetStrategy get = new GetStrategy(new GetStrategy.GetStrategyDeps(strategies));
        UpdateStrategy update =
                new UpdateStrategy(new UpdateStrategy.UpdateStrategyDeps(strategies, events));
        DeleteStrategy delete = new DeleteStrategy(new DeleteStrategy.DeleteStrategyDeps(strategies));
        http =
                new StrategyRestAdapter(
                        new StrategyRestAdapter.StrategyRestAdapterDeps(
                                create, list, get, update, delete));
    }

    @Test
    void updates_rules_as_new_version_keeps_name_and_emits_event() {
        CreateStrategyHttpResponse created = createNamed();
        events.clear();

        UpdateStrategyHttpResponse updated =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(created.strategyId()),
                                new UpdateStrategyHttpRequest(List.of(priceUnderHold("r2")))));

        assertEquals(STATUS_OK, updated.status());
        assertEquals(created.strategyId(), updated.strategyId());
        assertEquals(VERSION_TWO, updated.versionNumber());

        StrategyDetailHttpResponse detail =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId())));
        assertEquals(NAME, detail.name());
        assertEquals(VERSION_TWO, detail.versionNumber());
        assertEquals(CONDITION_PRICE_UNDER, detail.rules().getFirst().conditionType());
        assertEquals(THRESHOLD_NEW, detail.rules().getFirst().threshold());

        assertEquals(1, events.published().size());
        StrategyVersionPublished event =
                (StrategyVersionPublished) events.published().getFirst();
        assertEquals(created.strategyId(), event.strategyId());
        assertEquals(VERSION_TWO, event.versionNumber());
        assertEquals(OWNER, event.ownerId());
    }

    @Test
    void update_wrong_owner_or_unknown_id_returns_not_found() {
        CreateStrategyHttpResponse created = createNamed();
        UpdateStrategyHttpResponse wrongOwner =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OTHER,
                                new StrategyId(created.strategyId()),
                                new UpdateStrategyHttpRequest(List.of(priceUnderHold("r2")))));
        UpdateStrategyHttpResponse unknown =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(UNKNOWN_ID),
                                new UpdateStrategyHttpRequest(List.of(priceUnderHold("r2")))));
        assertEquals(STATUS_NOT_FOUND, wrongOwner.status());
        assertEquals(STATUS_NOT_FOUND, unknown.status());
        assertEquals(
                VERSION_ONE,
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId())))
                        .versionNumber());
    }

    @Test
    void update_empty_or_invalid_rules_returns_bad_request() {
        CreateStrategyHttpResponse created = createNamed();
        UpdateStrategyHttpResponse emptyRules =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(created.strategyId()),
                                new UpdateStrategyHttpRequest(List.of())));
        UpdateStrategyHttpResponse invalid =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(created.strategyId()),
                                new UpdateStrategyHttpRequest(
                                        List.of(
                                                new CreateStrategyHttpRequest.RuleBody(
                                                        "r1",
                                                        "moon_phase",
                                                        ACTION_HOLD,
                                                        INSTRUMENT,
                                                        EMPTY,
                                                        THRESHOLD,
                                                        EMPTY)))));
        assertEquals(STATUS_BAD_REQUEST, emptyRules.status());
        assertEquals(STATUS_BAD_REQUEST, invalid.status());
    }

    @Test
    void deletes_strategy_for_owner() {
        CreateStrategyHttpResponse created = createNamed();
        DeleteStrategyHttpResponse deleted =
                http.delete(
                        new StrategyRestAdapter.DeleteStrategyHttpInput(
                                OWNER, new StrategyId(created.strategyId())));
        assertEquals(STATUS_OK, deleted.status());
        assertEquals(created.strategyId(), deleted.strategyId());
        assertEquals(
                STATUS_NOT_FOUND,
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId()))).status());
        assertTrue(strategies.get(new StrategyId(created.strategyId())).isEmpty());
    }

    @Test
    void delete_wrong_owner_returns_not_found() {
        CreateStrategyHttpResponse created = createNamed();
        DeleteStrategyHttpResponse deleted =
                http.delete(
                        new StrategyRestAdapter.DeleteStrategyHttpInput(
                                OTHER, new StrategyId(created.strategyId())));
        assertEquals(STATUS_NOT_FOUND, deleted.status());
        assertTrue(strategies.get(new StrategyId(created.strategyId())).isPresent());
    }

    @Test
    void blank_owner_on_update_or_delete_returns_bad_request() {
        CreateStrategyHttpResponse created = createNamed();
        UpdateStrategyHttpResponse update =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                " ",
                                new StrategyId(created.strategyId()),
                                new UpdateStrategyHttpRequest(List.of(priceUnderHold("r2")))));
        DeleteStrategyHttpResponse delete =
                http.delete(
                        new StrategyRestAdapter.DeleteStrategyHttpInput(
                                " ", new StrategyId(created.strategyId())));
        assertEquals(STATUS_BAD_REQUEST, update.status());
        assertEquals(STATUS_BAD_REQUEST, delete.status());
    }

    private CreateStrategyHttpResponse createNamed() {
        CreateStrategyHttpResponse response =
                http.create(
                        new CreateStrategyHttpRequest(
                                OWNER, NAME, List.of(priceAboveHold("r1"))));
        assertEquals(STATUS_CREATED, response.status());
        return response;
    }

    private static CreateStrategyHttpRequest.RuleBody priceAboveHold(String id) {
        return new CreateStrategyHttpRequest.RuleBody(
                id, CONDITION_PRICE_ABOVE, ACTION_HOLD, INSTRUMENT, EMPTY, THRESHOLD, EMPTY);
    }

    private static CreateStrategyHttpRequest.RuleBody priceUnderHold(String id) {
        return new CreateStrategyHttpRequest.RuleBody(
                id, CONDITION_PRICE_UNDER, ACTION_HOLD, INSTRUMENT, EMPTY, THRESHOLD_NEW, EMPTY);
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {
        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }

        private List<DomainEvent> published() {
            return published;
        }

        private void clear() {
            published.clear();
        }
    }
}
