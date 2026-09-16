package com.defistrategyarena.strategy.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.strategy.adapter.persistence.JooqStrategyRepository;
import com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Strategies;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
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
    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000099";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String THRESHOLD = "3000";
    private static final String THRESHOLD_NEW = "2500";
    private static final String ALLOCATION = "10";
    private static final String INDICATOR = "RSI";
    private static final int VERSION_ONE = 1;
    private static final int VERSION_TWO = 2;
    private static final int PAGE_SIZE = 1;
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
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
        Strategy first = createStrategy(OWNER, NAME_A, priceAboveHold("r1"));
        repository.save(first);

        Optional<Strategy> loaded = repository.get(first.id());
        assertTrue(loaded.isPresent());
        assertEquals(VERSION_ONE, loaded.get().current().number());
        assertEquals(NAME_A, loaded.get().current().definition().name());

        assertTrue(
                repository
                        .findByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A))
                        .isPresent());
        assertEquals(SINGLE, repository.countByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A)));
        assertEquals(SINGLE, repository.size());

        Strategy second = createStrategy(OWNER, NAME_B, buyRule("r2"));
        repository.save(second);
        Strategy other = createStrategy(OTHER_OWNER, NAME_A, indicatorAboveHold("r3"));
        repository.save(other);

        StrategyPage page =
                repository.listByOwner(
                        ListStrategiesQuery.create(
                                new ListStrategiesQuery(
                                        OWNER,
                                        ListStrategiesQuery.DEFAULT_PAGE,
                                        PAGE_SIZE,
                                        ListStrategiesQuery.SORT_NAME,
                                        ListStrategiesQuery.ORDER_ASC)));
        assertEquals(2L, page.totalElements());
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
                        new Strategy.PublishNewVersionData(List.of(priceUnderHold("r1"))));
        repository.update(updated);
        assertEquals(VERSION_TWO, repository.get(first.id()).orElseThrow().current().number());

        assertThrows(
                DuplicateStrategyException.class,
                () -> repository.save(createStrategy(OWNER, "Alpha", priceAboveHold("dup"))));

        repository.delete(first.id());
        assertTrue(repository.get(first.id()).isEmpty());
        assertEquals(EMPTY, repository.countByOwnerAndName(new OwnerStrategyName(OWNER, NAME_A)));

        repository.delete(new StrategyId(UNKNOWN_ID));
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(createStrategy(OWNER, "missing-row", priceAboveHold("g"))));
    }

    @Test
    void rejects_null_dsl() {
        assertThrows(IllegalArgumentException.class, () -> new JooqStrategyRepository(null));
    }

    private static Strategy createStrategy(
            String ownerId, String name, StrategyDefinition.Rule rule) {
        return Strategy.create(
                new Strategy.CreateStrategyData(
                        ownerId, new StrategyDefinition(name, List.of(rule))));
    }

    private static StrategyDefinition.Rule priceAboveHold(String id) {
        return new StrategyDefinition.Rule(
                id,
                new StrategyDefinition.PriceAbove(INSTRUMENT, THRESHOLD),
                new StrategyDefinition.Hold());
    }

    private static StrategyDefinition.Rule priceUnderHold(String id) {
        return new StrategyDefinition.Rule(
                id,
                new StrategyDefinition.PriceUnder(INSTRUMENT, THRESHOLD_NEW),
                new StrategyDefinition.Hold());
    }

    private static StrategyDefinition.Rule buyRule(String id) {
        return new StrategyDefinition.Rule(
                id,
                new StrategyDefinition.PriceAbove(INSTRUMENT, THRESHOLD),
                new StrategyDefinition.Buy(INSTRUMENT, ALLOCATION));
    }

    private static StrategyDefinition.Rule indicatorAboveHold(String id) {
        return new StrategyDefinition.Rule(
                id,
                new StrategyDefinition.IndicatorAbove(INDICATOR, THRESHOLD),
                new StrategyDefinition.Hold());
    }
}
