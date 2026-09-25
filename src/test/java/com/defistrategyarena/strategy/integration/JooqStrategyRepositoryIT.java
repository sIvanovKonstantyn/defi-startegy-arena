package com.defistrategyarena.strategy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.strategy.adapter.persistence.JooqStrategyRepository;
import com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Strategies;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.domain.CompareOperator;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JooqStrategyRepositoryIT {

    private static final String OWNER = "owner-it";
    private static final String OTHER_OWNER = "other-it";
    private static final String NAME_A = "alpha";
    private static final String NAME_B = "beta";
    private static final String NAME_LP = "lp-tree";
    private static final String NAME_OR = "or-tree";
    private static final String NAME_DUPLICATE = "Alpha";
    private static final String NAME_MISSING = "missing-row";
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String RULE_ID = "r1";
    private static final int VERSION_ONE = 1;
    private static final int VERSION_TWO = 2;
    private static final int PAGE_SIZE = 1;
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
    private static final int TWO_CHILDREN = 2;
    private static final int SECOND_INDEX = 1;
    private static final long TOTAL_TWO = 2L;
    private static final String DB_NAME = "strategy_repo_it";

    private HikariDataSource dataSource;
    private JooqStrategyRepository repository;

    @BeforeEach
    void setUp() {
        dataSource = H2PostgresModeSupport.dataSource(new H2PostgresModeSupport.DatabaseName(DB_NAME));
        DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        dsl.deleteFrom(Strategies.STRATEGIES).execute();
        repository = new JooqStrategyRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void crud_round_trip_with_pagination_and_duplicate_rejection() {
        Strategy first = createStrategy(OWNER, NAME_A, StrategyTestFixtures.priceGtHold(RULE_ID));
        repository.save(first);

        Optional<Strategy> loaded = repository.get(first.id());
        assertTrue(loaded.isPresent());
        assertEquals(VERSION_ONE, loaded.get().current().number());
        assertEquals(NAME_A, loaded.get().current().definition().name());
        assertEquals(
                StrategyTestFixtures.DESCRIPTION, loaded.get().current().definition().description());

        assertTrue(repository.findByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A)).isPresent());
        assertEquals(SINGLE, repository.countByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A)));
        assertEquals(SINGLE, repository.size());

        repository.save(createStrategy(OWNER, NAME_B, StrategyTestFixtures.priceGtBuy("r2")));
        repository.save(
                createStrategy(OTHER_OWNER, NAME_A, StrategyTestFixtures.indicatorLtSell("r3")));

        StrategyPage page =
                repository.listByOwner(
                        ListStrategiesQuery.create(
                                new ListStrategiesQuery(
                                        OWNER,
                                        ListStrategiesQuery.DEFAULT_PAGE,
                                        PAGE_SIZE,
                                        ListStrategiesQuery.SORT_NAME,
                                        ListStrategiesQuery.ORDER_ASC)));
        assertEquals(TOTAL_TWO, page.totalElements());
        assertEquals(SINGLE, page.items().size());
        assertEquals(NAME_A, page.items().getFirst().current().definition().name());

        StrategyPage desc =
                repository.listByOwner(
                        ListStrategiesQuery.create(
                                new ListStrategiesQuery(
                                        OWNER,
                                        ListStrategiesQuery.DEFAULT_PAGE,
                                        PAGE_SIZE,
                                        ListStrategiesQuery.SORT_STRATEGY_ID,
                                        ListStrategiesQuery.ORDER_DESC)));
        assertEquals(SINGLE, desc.items().size());

        Strategy updated =
                first.publishNewVersion(
                        new Strategy.PublishNewVersionData(
                                StrategyTestFixtures.DESCRIPTION,
                                List.of(StrategyTestFixtures.priceLtHold(RULE_ID))));
        repository.update(updated);
        Strategy reloaded = repository.get(first.id()).orElseThrow();
        assertEquals(VERSION_TWO, reloaded.current().number());
        StrategyDefinition.PriceCompare when =
                assertInstanceOf(
                        StrategyDefinition.PriceCompare.class,
                        reloaded.current().definition().rules().getFirst().when());
        assertEquals(CompareOperator.LT, when.operator());
        assertEquals(StrategyTestFixtures.THRESHOLD_LOW, when.threshold());

        assertThrows(
                DuplicateStrategyException.class,
                () ->
                        repository.save(
                                createStrategy(
                                        OWNER,
                                        NAME_DUPLICATE,
                                        StrategyTestFixtures.priceGtHold("dup"))));

        repository.delete(first.id());
        assertTrue(repository.get(first.id()).isEmpty());
        assertEquals(EMPTY, repository.countByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A)));

        repository.delete(new StrategyId(UNKNOWN_ID));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        repository.update(
                                createStrategy(
                                        OWNER, NAME_MISSING, StrategyTestFixtures.priceGtHold("g"))));
    }

    @Test
    void round_trips_and_tree_with_indicator_and_open_lp() {
        Strategy strategy = createStrategy(OWNER, NAME_LP, StrategyTestFixtures.andOpenLp(RULE_ID));
        repository.save(strategy);

        StrategyDefinition.Rule rule =
                repository
                        .get(strategy.id())
                        .orElseThrow()
                        .current()
                        .definition()
                        .rules()
                        .getFirst();

        assertEquals(RULE_ID, rule.id());
        StrategyDefinition.And when = assertInstanceOf(StrategyDefinition.And.class, rule.when());
        assertEquals(TWO_CHILDREN, when.children().size());
        StrategyDefinition.IndicatorCompare indicator =
                assertInstanceOf(
                        StrategyDefinition.IndicatorCompare.class, when.children().getFirst());
        assertEquals(StrategyTestFixtures.INDICATOR_SMA, indicator.indicatorId().value());
        assertEquals(CompareOperator.LT, indicator.operator());
        assertEquals(StrategyTestFixtures.PERIOD_PARAMS, indicator.parameters());
        StrategyDefinition.PriceCompare price =
                assertInstanceOf(
                        StrategyDefinition.PriceCompare.class, when.children().get(SECOND_INDEX));
        assertEquals(StrategyTestFixtures.INSTRUMENT, price.instrument());
        assertEquals(CompareOperator.GT, price.operator());
        StrategyDefinition.OpenLp openLp =
                assertInstanceOf(StrategyDefinition.OpenLp.class, rule.then());
        assertEquals(StrategyTestFixtures.INSTRUMENT_PAIR, openLp.instrumentPair());
        assertEquals(StrategyTestFixtures.ALLOCATION, openLp.allocationPercent());
        assertEquals(StrategyTestFixtures.YEARLY_FEE, openLp.yearlyFeePercent());
    }

    @Test
    void round_trips_or_tree() {
        Strategy strategy = createStrategy(OWNER, NAME_OR, StrategyTestFixtures.orHold(RULE_ID));
        repository.save(strategy);

        StrategyDefinition.Or when =
                assertInstanceOf(
                        StrategyDefinition.Or.class,
                        repository
                                .get(strategy.id())
                                .orElseThrow()
                                .current()
                                .definition()
                                .rules()
                                .getFirst()
                                .when());
        assertEquals(TWO_CHILDREN, when.children().size());
        assertEquals(
                CompareOperator.GTE,
                assertInstanceOf(StrategyDefinition.PriceCompare.class, when.children().getFirst())
                        .operator());
        assertEquals(
                CompareOperator.LTE,
                assertInstanceOf(
                                StrategyDefinition.PriceCompare.class,
                                when.children().get(SECOND_INDEX))
                        .operator());
    }

    @Test
    void rejects_null_dsl() {
        assertThrows(IllegalArgumentException.class, () -> new JooqStrategyRepository(null));
    }

    @Test
    void load_rejects_missing_rule_graph_and_root() {
        Strategy strategy = createStrategy(OWNER, NAME_A, StrategyTestFixtures.priceGtHold(RULE_ID));
        repository.save(strategy);
        DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        dsl.execute("DELETE FROM strategy_rules");
        assertThrows(IllegalStateException.class, () -> repository.get(strategy.id()));

        repository.save(createStrategy(OWNER, NAME_B, StrategyTestFixtures.priceGtHold("r2")));
        Strategy second = repository.findByOwnerAndName(new OwnerStrategyName(OWNER, NAME_B)).orElseThrow();
        dsl.execute("DELETE FROM strategy_rule_conditions");
        dsl.execute("SET REFERENTIAL_INTEGRITY FALSE");
        dsl.execute(
                "INSERT INTO strategy_rule_conditions ("
                        + "condition_id, rule_row_id, parent_condition_id, sort_order, node_type)"
                        + " SELECT RANDOM_UUID(), rule_row_id, RANDOM_UUID(), 0, 'price_compare'"
                        + " FROM strategy_rules WHERE strategy_id = ?",
                java.util.UUID.fromString(second.id().value()));
        dsl.execute("SET REFERENTIAL_INTEGRITY TRUE");
        assertThrows(IllegalStateException.class, () -> repository.get(second.id()));
    }

    @Test
    void save_rejects_indicator_missing_from_database() {
        Strategy strategy =
                createStrategy(OWNER, NAME_LP, StrategyTestFixtures.andOpenLp(RULE_ID));
        DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        dsl.execute("SET REFERENTIAL_INTEGRITY FALSE");
        dsl.execute("DELETE FROM indicators");
        dsl.execute("SET REFERENTIAL_INTEGRITY TRUE");
        assertThrows(IllegalArgumentException.class, () -> repository.save(strategy));
    }

    @Test
    void load_rejects_orphan_indicator_reference() {
        Strategy strategy =
                createStrategy(OWNER, NAME_LP, StrategyTestFixtures.andOpenLp(RULE_ID));
        repository.save(strategy);
        DSLContext dsl = H2PostgresModeSupport.migratedDsl(dataSource);
        dsl.execute("SET REFERENTIAL_INTEGRITY FALSE");
        dsl.execute(
                "UPDATE strategy_rule_conditions SET indicator_id = ?"
                        + " WHERE indicator_id IS NOT NULL",
                java.util.UUID.fromString("b0000000-0000-4000-8000-000000000099"));
        dsl.execute("SET REFERENTIAL_INTEGRITY TRUE");
        assertThrows(IllegalArgumentException.class, () -> repository.get(strategy.id()));
    }

    private static Strategy createStrategy(
            String ownerId, String name, StrategyDefinition.Rule rule) {
        return Strategy.create(
                new Strategy.CreateStrategyData(
                        ownerId, StrategyTestFixtures.definition(name, rule)));
    }
}
