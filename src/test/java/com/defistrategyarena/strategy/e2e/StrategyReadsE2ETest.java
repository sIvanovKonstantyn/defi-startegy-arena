package com.defistrategyarena.strategy.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventPublisher;
import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyDetailHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyListHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.adapter.web.StrategySummaryHttpResponse;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.DeleteStrategy;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrategyReadsE2ETest {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int PAGE_SIZE_ONE = 1;
    private static final int TOTAL_TWO = 2;
    private static final int TOTAL_PAGES_TWO = 2;
    private static final int PAGE_BEYOND = 5;
    private static final String OWNER = "owner-reads";
    private static final String OTHER_OWNER = "other-owner";
    private static final String NAME_A = "alpha";
    private static final String NAME_B = "beta";
    private static final String EMPTY = "";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String CONDITION_PRICE_ABOVE = "price_above";
    private static final String CONDITION_PRICE_UNDER = "price_under";
    private static final String CONDITION_INDICATOR_BELOW = "indicator_below";
    private static final String CONDITION_INDICATOR_ABOVE = "indicator_above";
    private static final String ACTION_HOLD = "hold";
    private static final String ACTION_BUY = "buy";
    private static final String ACTION_SELL = "sell";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String INDICATOR = "RSI";
    private static final String THRESHOLD = "3000";
    private static final String ALLOCATION = "10";

    private InMemoryStrategyRepository strategies;
    private StrategyRestAdapter http;

    @BeforeEach
    void setUp() {
        strategies = new InMemoryStrategyRepository();
        DomainEventPublisher events = new RecordingEventPublisher();
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
    void lists_strategies_sorted_by_name_with_pagination() {
        createNamed(NAME_B);
        createNamed(NAME_A);

        StrategyListHttpResponse firstPage =
                http.list(
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                PAGE_SIZE_ONE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        StrategyListHttpResponse secondPage =
                http.list(
                        new ListStrategiesQuery(
                                OWNER,
                                1,
                                PAGE_SIZE_ONE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));

        assertEquals(STATUS_OK, firstPage.status());
        assertEquals(TOTAL_TWO, firstPage.totalElements());
        assertEquals(TOTAL_PAGES_TWO, firstPage.totalPages());
        assertEquals(NAME_A, firstPage.items().getFirst().name());
        assertEquals(NAME_B, secondPage.items().getFirst().name());
    }

    @Test
    void lists_strategies_sorted_by_strategy_id_desc() {
        CreateStrategyHttpResponse first = createNamed(NAME_A);
        CreateStrategyHttpResponse second = createNamed(NAME_B);
        StrategyListHttpResponse response =
                http.list(
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_STRATEGY_ID,
                                ListStrategiesQuery.ORDER_DESC));
        assertEquals(STATUS_OK, response.status());
        List<String> ids = response.items().stream().map(StrategySummaryHttpResponse::strategyId).toList();
        assertTrue(ids.getFirst().compareTo(ids.get(1)) > 0);
        assertTrue(ids.contains(first.strategyId()));
        assertTrue(ids.contains(second.strategyId()));
    }

    @Test
    void lists_empty_page_when_page_beyond_results() {
        createNamed(NAME_A);
        StrategyListHttpResponse response =
                http.list(
                        new ListStrategiesQuery(
                                OWNER,
                                PAGE_BEYOND,
                                PAGE_SIZE_ONE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertEquals(STATUS_OK, response.status());
        assertEquals(1L, response.totalElements());
        assertTrue(response.items().isEmpty());
    }

    @Test
    void lists_empty_for_unknown_owner() {
        createNamed(NAME_A);
        StrategyListHttpResponse response =
                http.list(
                        new ListStrategiesQuery(
                                OTHER_OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertEquals(STATUS_OK, response.status());
        assertEquals(0L, response.totalElements());
        assertTrue(response.items().isEmpty());
    }

    @Test
    void rejects_invalid_list_query_variants() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                -1,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                null,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                EMPTY,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                0,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                101,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                "createdAt",
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                "up"));
    }

    @Test
    void list_query_defaults_null_sort_and_order() {
        ListStrategiesQuery query =
                new ListStrategiesQuery(
                        OWNER, ListStrategiesQuery.DEFAULT_PAGE, ListStrategiesQuery.DEFAULT_SIZE, null, null);
        ListStrategiesQuery created = ListStrategiesQuery.create(query);
        assertEquals(ListStrategiesQuery.SORT_NAME, created.sort());
        assertEquals(ListStrategiesQuery.ORDER_ASC, created.order());
    }

    @Test
    void gets_strategy_detail_with_rules() {
        CreateStrategyHttpResponse created = createNamed(NAME_A);
        StrategyDetailHttpResponse detail =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId())));
        assertEquals(STATUS_OK, detail.status());
        assertEquals(NAME_A, detail.name());
        assertEquals(1, detail.rules().size());
        assertEquals(CONDITION_PRICE_ABOVE, detail.rules().getFirst().conditionType());
        assertTrue(strategies.get(new StrategyId(created.strategyId())).isPresent());
    }

    @Test
    void gets_detail_for_all_dsl_variants() {
        CreateStrategyHttpResponse under =
                http.create(
                        new CreateStrategyHttpRequest(
                                OWNER,
                                "under",
                                List.of(
                                        new CreateStrategyHttpRequest.RuleBody(
                                                "r1",
                                                CONDITION_PRICE_UNDER,
                                                ACTION_SELL,
                                                INSTRUMENT,
                                                EMPTY,
                                                THRESHOLD,
                                                ALLOCATION))));
        CreateStrategyHttpResponse below =
                http.create(
                        new CreateStrategyHttpRequest(
                                OWNER,
                                "below",
                                List.of(
                                        new CreateStrategyHttpRequest.RuleBody(
                                                "r1",
                                                CONDITION_INDICATOR_BELOW,
                                                ACTION_HOLD,
                                                EMPTY,
                                                INDICATOR,
                                                THRESHOLD,
                                                EMPTY))));
        CreateStrategyHttpResponse above =
                http.create(
                        new CreateStrategyHttpRequest(
                                OWNER,
                                "above",
                                List.of(
                                        new CreateStrategyHttpRequest.RuleBody(
                                                "r1",
                                                CONDITION_INDICATOR_ABOVE,
                                                ACTION_BUY,
                                                INSTRUMENT,
                                                INDICATOR,
                                                THRESHOLD,
                                                ALLOCATION))));
        assertEquals(STATUS_CREATED, under.status());
        assertEquals(STATUS_CREATED, below.status());
        assertEquals(STATUS_CREATED, above.status());
        assertEquals(
                CONDITION_PRICE_UNDER,
                http.get(new GetStrategyQuery(OWNER, new StrategyId(under.strategyId())))
                        .rules()
                        .getFirst()
                        .conditionType());
        assertEquals(
                CONDITION_INDICATOR_BELOW,
                http.get(new GetStrategyQuery(OWNER, new StrategyId(below.strategyId())))
                        .rules()
                        .getFirst()
                        .conditionType());
        assertEquals(
                ACTION_BUY,
                http.get(new GetStrategyQuery(OWNER, new StrategyId(above.strategyId())))
                        .rules()
                        .getFirst()
                        .actionType());
    }

    @Test
    void get_wrong_owner_or_unknown_id_returns_not_found() {
        CreateStrategyHttpResponse created = createNamed(NAME_A);
        StrategyDetailHttpResponse wrongOwner =
                http.get(new GetStrategyQuery(OTHER_OWNER, new StrategyId(created.strategyId())));
        StrategyDetailHttpResponse unknown =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(UNKNOWN_ID)));
        assertEquals(STATUS_NOT_FOUND, wrongOwner.status());
        assertEquals(STATUS_NOT_FOUND, unknown.status());
    }

    @Test
    void get_blank_owner_or_null_id_rejected() {
        CreateStrategyHttpResponse created = createNamed(NAME_A);
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetStrategyQuery(" ", new StrategyId(created.strategyId())));
        assertThrows(IllegalArgumentException.class, () -> new GetStrategyQuery(null, new StrategyId(created.strategyId())));
        assertThrows(IllegalArgumentException.class, () -> new GetStrategyQuery(OWNER, null));
        GetStrategyQuery query = new GetStrategyQuery(OWNER, new StrategyId(created.strategyId()));
        assertEquals(OWNER, GetStrategyQuery.create(query).ownerId());
    }

    @Test
    void strategy_page_and_http_dto_factories() {
        StrategyPage page = StrategyPage.create(new StrategyPage(List.of(), 0L));
        assertTrue(page.items().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new StrategyPage(null, 0L));
        StrategyListHttpResponse list =
                StrategyListHttpResponse.create(
                        new StrategyListHttpResponse(
                                STATUS_OK,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                0L,
                                0,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC,
                                List.of()));
        assertEquals(STATUS_OK, list.status());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyListHttpResponse(
                                STATUS_OK,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                0L,
                                0,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC,
                                null));
        StrategyDetailHttpResponse detail =
                StrategyDetailHttpResponse.create(
                        new StrategyDetailHttpResponse(STATUS_OK, "id", NAME_A, "PRIVATE", 1, List.of()));
        assertEquals(NAME_A, detail.name());
        assertThrows(
                IllegalArgumentException.class,
                () -> new StrategyDetailHttpResponse(STATUS_OK, "id", NAME_A, "PRIVATE", 1, null));
        StrategySummaryHttpResponse summary =
                StrategySummaryHttpResponse.create(
                        new StrategySummaryHttpResponse("id", NAME_A, "PRIVATE", 1));
        assertEquals(NAME_A, summary.name());
    }

    private CreateStrategyHttpResponse createNamed(String name) {
        CreateStrategyHttpResponse response =
                http.create(
                        new CreateStrategyHttpRequest(
                                OWNER,
                                name,
                                List.of(
                                        new CreateStrategyHttpRequest.RuleBody(
                                                "r1",
                                                CONDITION_PRICE_ABOVE,
                                                ACTION_HOLD,
                                                INSTRUMENT,
                                                EMPTY,
                                                THRESHOLD,
                                                EMPTY))));
        assertEquals(STATUS_CREATED, response.status());
        return response;
    }

    private static final class RecordingEventPublisher implements DomainEventPublisher {
        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    }
}
