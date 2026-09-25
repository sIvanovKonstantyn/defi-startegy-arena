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
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
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
    private static final int SINGLE_RULE = 1;
    private static final int TWO_CHILDREN = 2;
    private static final int VERSION_ONE = 1;
    private static final int SECOND_INDEX = 1;
    private static final String OWNER = "owner-reads";
    private static final String OTHER_OWNER = "other-owner";
    private static final String NAME_A = "alpha";
    private static final String NAME_B = "beta";
    private static final String NAME_LP = "lp-pair";
    private static final String NAME_INDICATOR = "indicator-sell";
    private static final String EMPTY = "";
    private static final String BLANK = " ";
    private static final String STRATEGY_ID = "id";
    private static final String PRIVACY_PRIVATE = "PRIVATE";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String RULE_ID = "r1";
    private static final String SORT_CREATED_AT = "createdAt";
    private static final String ORDER_UP = "up";
    private static final int NEGATIVE_PAGE = -1;
    private static final int ZERO_SIZE = 0;
    private static final int ZERO_PAGES = 0;
    private static final int ASCENDING = 0;
    private static final int OVERSIZED_PAGE = 101;
    private static final long ZERO_TOTAL = 0L;
    private static final long ONE_TOTAL = 1L;

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
                                new StrategyUseCases(create, list, get, update, delete)));
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
                                SECOND_INDEX,
                                PAGE_SIZE_ONE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));

        assertEquals(STATUS_OK, firstPage.status());
        assertEquals(TOTAL_TWO, firstPage.totalElements());
        assertEquals(TOTAL_PAGES_TWO, firstPage.totalPages());
        assertEquals(NAME_A, firstPage.items().getFirst().name());
        assertEquals(StrategyTestFixtures.DESCRIPTION, firstPage.items().getFirst().description());
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
        List<String> ids =
                response.items().stream().map(StrategySummaryHttpResponse::strategyId).toList();
        assertTrue(ids.getFirst().compareTo(ids.get(SECOND_INDEX)) > ASCENDING);
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
        assertEquals(ONE_TOTAL, response.totalElements());
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
        assertEquals(ZERO_TOTAL, response.totalElements());
        assertTrue(response.items().isEmpty());
    }

    @Test
    void rejects_invalid_list_query_variants() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                NEGATIVE_PAGE,
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
                                ZERO_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                OVERSIZED_PAGE,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                SORT_CREATED_AT,
                                ListStrategiesQuery.ORDER_ASC));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ListStrategiesQuery(
                                OWNER,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ListStrategiesQuery.SORT_NAME,
                                ORDER_UP));
    }

    @Test
    void list_query_defaults_null_sort_and_order() {
        ListStrategiesQuery query =
                new ListStrategiesQuery(
                        OWNER,
                        ListStrategiesQuery.DEFAULT_PAGE,
                        ListStrategiesQuery.DEFAULT_SIZE,
                        null,
                        null);
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
        assertEquals(StrategyTestFixtures.DESCRIPTION, detail.description());
        assertEquals(VERSION_ONE, detail.versionNumber());
        assertEquals(SINGLE_RULE, detail.rules().size());
        CreateStrategyHttpRequest.RuleBody rule = detail.rules().getFirst();
        assertEquals(RULE_ID, rule.id());
        assertEquals(StrategyTestFixtures.CONDITION_PRICE_COMPARE, rule.when().type());
        assertEquals(StrategyTestFixtures.OPERATOR_GT, rule.when().operator());
        assertEquals(StrategyTestFixtures.THRESHOLD, rule.when().threshold());
        assertEquals(StrategyTestFixtures.ACTION_HOLD, rule.then().type());
        assertTrue(strategies.get(new StrategyId(created.strategyId())).isPresent());
    }

    @Test
    void gets_detail_for_and_tree_with_open_lp() {
        CreateStrategyHttpResponse created =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        NAME_LP, StrategyTestFixtures.andOpenLpBody(RULE_ID))));
        assertEquals(STATUS_CREATED, created.status());

        CreateStrategyHttpRequest.RuleBody rule =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId())))
                        .rules()
                        .getFirst();

        assertEquals(StrategyTestFixtures.CONDITION_AND, rule.when().type());
        assertEquals(TWO_CHILDREN, rule.when().children().size());
        CreateStrategyHttpRequest.ConditionBody indicator = rule.when().children().getFirst();
        assertEquals(StrategyTestFixtures.CONDITION_INDICATOR_COMPARE, indicator.type());
        assertEquals(StrategyTestFixtures.INDICATOR_SMA, indicator.indicator());
        assertEquals(StrategyTestFixtures.PERIOD_PARAMS, indicator.parameters());
        CreateStrategyHttpRequest.ConditionBody price = rule.when().children().get(SECOND_INDEX);
        assertEquals(StrategyTestFixtures.CONDITION_PRICE_COMPARE, price.type());
        assertEquals(StrategyTestFixtures.INSTRUMENT, price.instrument());
        assertEquals(StrategyTestFixtures.ACTION_OPEN_LP, rule.then().type());
        assertEquals(StrategyTestFixtures.INSTRUMENT_PAIR, rule.then().instrumentPair());
        assertEquals(StrategyTestFixtures.YEARLY_FEE, rule.then().yearlyFeePercent());
    }

    @Test
    void gets_detail_for_or_tree_and_indicator_sell() {
        CreateStrategyHttpResponse orCreated =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        NAME_B,
                                        StrategyTestFixtures.rule(
                                                RULE_ID,
                                                StrategyTestFixtures.group(
                                                        StrategyTestFixtures.CONDITION_OR,
                                                        List.of(
                                                                StrategyTestFixtures.priceCompare(
                                                                        StrategyTestFixtures
                                                                                .OPERATOR_GT,
                                                                        StrategyTestFixtures
                                                                                .THRESHOLD),
                                                                StrategyTestFixtures.priceCompare(
                                                                        StrategyTestFixtures
                                                                                .OPERATOR_LT,
                                                                        StrategyTestFixtures
                                                                                .THRESHOLD_LOW))),
                                                StrategyTestFixtures.hold()))));
        CreateStrategyHttpResponse sellCreated =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        NAME_INDICATOR,
                                        StrategyTestFixtures.rule(
                                                RULE_ID,
                                                StrategyTestFixtures.indicatorCompare(
                                                        StrategyTestFixtures.INDICATOR_RSI,
                                                        StrategyTestFixtures.OPERATOR_LT,
                                                        StrategyTestFixtures.THRESHOLD_LOW,
                                                        StrategyTestFixtures.PERIOD_PARAMS),
                                                StrategyTestFixtures.sell(
                                                        StrategyTestFixtures.ALLOCATION)))));
        assertEquals(STATUS_CREATED, orCreated.status());
        assertEquals(STATUS_CREATED, sellCreated.status());

        CreateStrategyHttpRequest.RuleBody orRule =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(orCreated.strategyId())))
                        .rules()
                        .getFirst();
        CreateStrategyHttpRequest.RuleBody sellRule =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(sellCreated.strategyId())))
                        .rules()
                        .getFirst();

        assertEquals(StrategyTestFixtures.CONDITION_OR, orRule.when().type());
        assertEquals(TWO_CHILDREN, orRule.when().children().size());
        assertEquals(StrategyTestFixtures.CONDITION_INDICATOR_COMPARE, sellRule.when().type());
        assertEquals(StrategyTestFixtures.ACTION_SELL, sellRule.then().type());
        assertEquals(StrategyTestFixtures.ALLOCATION, sellRule.then().allocationPercent());
    }

    @Test
    void gets_detail_for_buy_action() {
        CreateStrategyHttpResponse created =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        NAME_A,
                                        StrategyTestFixtures.rule(
                                                RULE_ID,
                                                StrategyTestFixtures.priceCompare(
                                                        StrategyTestFixtures.OPERATOR_GT,
                                                        StrategyTestFixtures.THRESHOLD),
                                                StrategyTestFixtures.buy(
                                                        StrategyTestFixtures.ALLOCATION)))));
        assertEquals(STATUS_CREATED, created.status());
        CreateStrategyHttpRequest.ActionBody then =
                http.get(new GetStrategyQuery(OWNER, new StrategyId(created.strategyId())))
                        .rules()
                        .getFirst()
                        .then();
        assertEquals(StrategyTestFixtures.ACTION_BUY, then.type());
        assertEquals(StrategyTestFixtures.INSTRUMENT, then.instrument());
        assertEquals(StrategyTestFixtures.ALLOCATION, then.allocationPercent());
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
        assertTrue(unknown.rules().isEmpty());
    }

    @Test
    void get_blank_owner_or_null_id_rejected() {
        CreateStrategyHttpResponse created = createNamed(NAME_A);
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetStrategyQuery(BLANK, new StrategyId(created.strategyId())));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GetStrategyQuery(null, new StrategyId(created.strategyId())));
        assertThrows(IllegalArgumentException.class, () -> new GetStrategyQuery(OWNER, null));
        GetStrategyQuery query = new GetStrategyQuery(OWNER, new StrategyId(created.strategyId()));
        assertEquals(OWNER, GetStrategyQuery.create(query).ownerId());
    }

    @Test
    void strategy_page_and_http_dto_factories() {
        StrategyPage page = StrategyPage.create(new StrategyPage(List.of(), ZERO_TOTAL));
        assertTrue(page.items().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new StrategyPage(null, ZERO_TOTAL));
        StrategyListHttpResponse list =
                StrategyListHttpResponse.create(
                        new StrategyListHttpResponse(
                                STATUS_OK,
                                ListStrategiesQuery.DEFAULT_PAGE,
                                ListStrategiesQuery.DEFAULT_SIZE,
                                ZERO_TOTAL,
                                ZERO_PAGES,
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
                                ZERO_TOTAL,
                                ZERO_PAGES,
                                ListStrategiesQuery.SORT_NAME,
                                ListStrategiesQuery.ORDER_ASC,
                                null));
        StrategyDetailHttpResponse detail =
                StrategyDetailHttpResponse.create(
                        new StrategyDetailHttpResponse(
                                STATUS_OK,
                                STRATEGY_ID,
                                NAME_A,
                                StrategyTestFixtures.DESCRIPTION,
                                PRIVACY_PRIVATE,
                                VERSION_ONE,
                                List.of(),
                                EMPTY,
                                EMPTY));
        assertEquals(NAME_A, detail.name());
        assertEquals(StrategyTestFixtures.DESCRIPTION, detail.description());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDetailHttpResponse(
                                STATUS_OK,
                                STRATEGY_ID,
                                NAME_A,
                                StrategyTestFixtures.DESCRIPTION,
                                PRIVACY_PRIVATE,
                                VERSION_ONE,
                                null,
                                EMPTY,
                                EMPTY));
        StrategySummaryHttpResponse summary =
                StrategySummaryHttpResponse.create(
                        new StrategySummaryHttpResponse(
                                STRATEGY_ID,
                                NAME_A,
                                StrategyTestFixtures.DESCRIPTION,
                                PRIVACY_PRIVATE,
                                VERSION_ONE));
        assertEquals(NAME_A, summary.name());
        assertEquals(StrategyTestFixtures.DESCRIPTION, summary.description());
    }

    private CreateStrategyHttpResponse createNamed(String name) {
        CreateStrategyHttpResponse response =
                http.create(
                        new StrategyRestAdapter.CreateStrategyHttpInput(
                                OWNER,
                                StrategyTestFixtures.createRequest(
                                        name, StrategyTestFixtures.priceGtHoldBody(RULE_ID))));
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
