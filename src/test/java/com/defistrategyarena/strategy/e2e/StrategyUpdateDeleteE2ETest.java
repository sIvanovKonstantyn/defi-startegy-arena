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
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpResponse;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
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
    private static final int SINGLE_EVENT = 1;
    private static final int TWO_CHILDREN = 2;
    private static final String OWNER = "owner-update";
    private static final String OTHER = "other-owner";
    private static final String NAME = "mutable-rules";
    private static final String BLANK = " ";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String CONDITION_UNKNOWN = "moon_phase";
    private static final String RULE_ID = "r1";
    private static final String RULE_ID_TWO = "r2";

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
                                new StrategyUseCases(create, list, get, update, delete)));
    }

    @Test
    void updates_rules_as_new_version_keeps_name_and_emits_event() {
        CreateStrategyHttpResponse created = createNamed();
        events.clear();

        UpdateStrategyHttpResponse updated =
                update(created.strategyId(), StrategyTestFixtures.priceLtHoldBody(RULE_ID_TWO));

        assertEquals(STATUS_OK, updated.status());
        assertEquals(created.strategyId(), updated.strategyId());
        assertEquals(VERSION_TWO, updated.versionNumber());

        StrategyDetailHttpResponse detail = detail(created.strategyId());
        assertEquals(NAME, detail.name());
        assertEquals(VERSION_TWO, detail.versionNumber());
        CreateStrategyHttpRequest.RuleBody rule = detail.rules().getFirst();
        assertEquals(RULE_ID_TWO, rule.id());
        assertEquals(StrategyTestFixtures.CONDITION_PRICE_COMPARE, rule.when().type());
        assertEquals(StrategyTestFixtures.OPERATOR_LT, rule.when().operator());
        assertEquals(StrategyTestFixtures.THRESHOLD_LOW, rule.when().threshold());

        assertEquals(SINGLE_EVENT, events.published().size());
        StrategyVersionPublished event = (StrategyVersionPublished) events.published().getFirst();
        assertEquals(created.strategyId(), event.strategyId());
        assertEquals(VERSION_TWO, event.versionNumber());
        assertEquals(OWNER, event.ownerId());
    }

    @Test
    void updates_to_and_tree_with_open_lp_action() {
        CreateStrategyHttpResponse created = createNamed();

        UpdateStrategyHttpResponse updated =
                update(created.strategyId(), StrategyTestFixtures.andOpenLpBody(RULE_ID_TWO));

        assertEquals(STATUS_OK, updated.status());
        CreateStrategyHttpRequest.RuleBody rule = detail(created.strategyId()).rules().getFirst();
        assertEquals(StrategyTestFixtures.CONDITION_AND, rule.when().type());
        assertEquals(TWO_CHILDREN, rule.when().children().size());
        assertEquals(StrategyTestFixtures.ACTION_OPEN_LP, rule.then().type());
        assertEquals(StrategyTestFixtures.INSTRUMENT_PAIR, rule.then().instrumentPair());
    }

    @Test
    void update_wrong_owner_or_unknown_id_returns_not_found() {
        CreateStrategyHttpResponse created = createNamed();
        UpdateStrategyHttpResponse wrongOwner =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OTHER,
                                new StrategyId(created.strategyId()),
                                StrategyTestFixtures.updateRequest(
                                        List.of(StrategyTestFixtures.priceLtHoldBody(RULE_ID_TWO)))));
        UpdateStrategyHttpResponse unknown =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(UNKNOWN_ID),
                                StrategyTestFixtures.updateRequest(
                                        List.of(StrategyTestFixtures.priceLtHoldBody(RULE_ID_TWO)))));
        assertEquals(STATUS_NOT_FOUND, wrongOwner.status());
        assertEquals(STATUS_NOT_FOUND, unknown.status());
        assertEquals(VERSION_ONE, detail(created.strategyId()).versionNumber());
    }

    @Test
    void update_empty_or_invalid_rules_returns_bad_request() {
        CreateStrategyHttpResponse created = createNamed();
        UpdateStrategyHttpResponse emptyRules =
                http.update(
                        new StrategyRestAdapter.UpdateStrategyHttpInput(
                                OWNER,
                                new StrategyId(created.strategyId()),
                                StrategyTestFixtures.updateRequest(List.of())));
        UpdateStrategyHttpResponse unknownCondition =
                update(
                        created.strategyId(),
                        StrategyTestFixtures.rule(
                                RULE_ID,
                                StrategyTestFixtures.group(CONDITION_UNKNOWN, List.of()),
                                StrategyTestFixtures.hold()));
        UpdateStrategyHttpResponse emptyAnd =
                update(
                        created.strategyId(),
                        StrategyTestFixtures.rule(
                                RULE_ID,
                                StrategyTestFixtures.group(
                                        StrategyTestFixtures.CONDITION_AND, List.of()),
                                StrategyTestFixtures.hold()));
        UpdateStrategyHttpResponse unknownIndicator =
                update(
                        created.strategyId(),
                        StrategyTestFixtures.rule(
                                RULE_ID,
                                StrategyTestFixtures.indicatorCompare(
                                        CONDITION_UNKNOWN,
                                        StrategyTestFixtures.OPERATOR_LT,
                                        StrategyTestFixtures.THRESHOLD,
                                        StrategyTestFixtures.PERIOD_PARAMS),
                                StrategyTestFixtures.hold()));

        assertEquals(STATUS_BAD_REQUEST, emptyRules.status());
        assertEquals(STATUS_BAD_REQUEST, unknownCondition.status());
        assertEquals(STATUS_BAD_REQUEST, emptyAnd.status());
        assertEquals(STATUS_BAD_REQUEST, unknownIndicator.status());
        assertEquals(VERSION_ONE, detail(created.strategyId()).versionNumber());
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
        assertEquals(STATUS_NOT_FOUND, detail(created.strategyId()).status());
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
                                BLANK,
                                new StrategyId(created.strategyId()),
                                StrategyTestFixtures.updateRequest(
                                        List.of(StrategyTestFixtures.priceLtHoldBody(RULE_ID_TWO)))));
        DeleteStrategyHttpResponse delete =
                http.delete(
                        new StrategyRestAdapter.DeleteStrategyHttpInput(
                                BLANK, new StrategyId(created.strategyId())));
        assertEquals(STATUS_BAD_REQUEST, update.status());
        assertEquals(STATUS_BAD_REQUEST, delete.status());
    }

    private CreateStrategyHttpResponse createNamed() {
        CreateStrategyHttpResponse response =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        NAME, StrategyTestFixtures.priceGtHoldBody(RULE_ID))));
        assertEquals(STATUS_CREATED, response.status());
        return response;
    }

    private UpdateStrategyHttpResponse update(
            String strategyId, CreateStrategyHttpRequest.RuleBody rule) {
        return http.update(
                new StrategyRestAdapter.UpdateStrategyHttpInput(
                        OWNER,
                        new StrategyId(strategyId),
                        StrategyTestFixtures.updateRequest(List.of(rule))));
    }

    private StrategyDetailHttpResponse detail(String strategyId) {
        return http.get(new GetStrategyQuery(OWNER, new StrategyId(strategyId)));
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

        private void clear() {
            published.clear();
        }
    }
}
