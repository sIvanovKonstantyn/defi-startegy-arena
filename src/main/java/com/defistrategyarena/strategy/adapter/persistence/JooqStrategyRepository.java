package com.defistrategyarena.strategy.adapter.persistence;

import static com.defistrategyarena.strategy.adapter.persistence.jooq.tables.Strategies.STRATEGIES;

import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.exception.DataAccessException;

public final class JooqStrategyRepository implements StrategyRepository {

    private static final int EMPTY_COUNT = 0;
    private static final int SINGLE_COUNT = 1;
    private static final String UNKNOWN_STRATEGY = "strategy not found";
    private static final String DSL_REQUIRED = "dsl context must not be null";

    private final DSLContext dsl;
    private final StrategyGraphStore graphStore;

    public JooqStrategyRepository(DSLContext dsl) {
        if (dsl == null) {
            throw new IllegalArgumentException(DSL_REQUIRED);
        }
        this.dsl = dsl;
        this.graphStore = new StrategyGraphStore(dsl);
    }

    @Override
    public void save(Strategy strategy) {
        try {
            insertRow(strategy);
            graphStore.replaceRules(strategy);
        } catch (DataAccessException ex) {
            throw DuplicateKeyMapper.map(ex);
        }
    }

    @Override
    public void update(Strategy strategy) {
        VersionPatch patch = VersionPatch.from(strategy);
        int updated =
                dsl.update(STRATEGIES)
                        .set(STRATEGIES.PRIVACY, patch.privacy())
                        .set(STRATEGIES.VERSION_NUMBER, patch.versionNumber())
                        .set(STRATEGIES.DESCRIPTION, patch.description())
                        .set(STRATEGIES.UPDATED_AT, patch.updatedAt())
                        .where(STRATEGIES.STRATEGY_ID.eq(patch.strategyId()))
                        .execute();
        if (updated == EMPTY_COUNT) {
            throw new IllegalArgumentException(UNKNOWN_STRATEGY);
        }
        graphStore.replaceRules(strategy);
    }

    @Override
    public void delete(StrategyId id) {
        dsl.deleteFrom(STRATEGIES).where(STRATEGIES.STRATEGY_ID.eq(uuid(id))).execute();
    }

    @Override
    public Optional<Strategy> get(StrategyId id) {
        return dsl.selectFrom(STRATEGIES)
                .where(STRATEGIES.STRATEGY_ID.eq(uuid(id)))
                .fetchOptional()
                .map(record -> JooqStrategyRowMapper.toStrategy(new JooqStrategyRowMapper.RowInput(record, graphStore)));
    }

    @Override
    public Optional<Strategy> findByOwnerAndName(OwnerStrategyName key) {
        return dsl.selectFrom(STRATEGIES)
                .where(STRATEGIES.OWNER_ID_NORMALIZED.eq(normalize(new NormalizeText(key.ownerId()))))
                .and(STRATEGIES.NAME_NORMALIZED.eq(normalize(new NormalizeText(key.name()))))
                .fetchOptional()
                .map(record -> JooqStrategyRowMapper.toStrategy(new JooqStrategyRowMapper.RowInput(record, graphStore)));
    }

    @Override
    public StrategyPage listByOwner(ListStrategiesQuery query) {
        long total = dsl.fetchCount(STRATEGIES, STRATEGIES.OWNER_ID.eq(query.ownerId()));
        List<Strategy> items =
                dsl.selectFrom(STRATEGIES)
                        .where(STRATEGIES.OWNER_ID.eq(query.ownerId()))
                        .orderBy(sortField(query))
                        .limit(query.size())
                        .offset(query.page() * query.size())
                        .fetch(
                                record ->
                                        JooqStrategyRowMapper.toStrategy(
                                                new JooqStrategyRowMapper.RowInput(record, graphStore)));
        return new StrategyPage(List.copyOf(new ArrayList<>(items)), total);
    }

    @Override
    public int size() {
        return dsl.fetchCount(STRATEGIES);
    }

    @Override
    public int countByOwnerAndName(OwnerStrategyName key) {
        int count =
                dsl.fetchCount(
                        STRATEGIES,
                        STRATEGIES.OWNER_ID_NORMALIZED
                                .eq(normalize(new NormalizeText(key.ownerId())))
                                .and(
                                        STRATEGIES.NAME_NORMALIZED.eq(
                                                normalize(new NormalizeText(key.name())))));
        if (count > EMPTY_COUNT) {
            return SINGLE_COUNT;
        }
        return EMPTY_COUNT;
    }

    private void insertRow(Strategy strategy) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String name = strategy.current().definition().name();
        dsl.insertInto(STRATEGIES)
                .set(STRATEGIES.STRATEGY_ID, uuid(strategy.id()))
                .set(STRATEGIES.OWNER_ID, strategy.ownerId())
                .set(STRATEGIES.OWNER_ID_NORMALIZED, normalize(new NormalizeText(strategy.ownerId())))
                .set(STRATEGIES.NAME, name)
                .set(STRATEGIES.NAME_NORMALIZED, normalize(new NormalizeText(name)))
                .set(STRATEGIES.DESCRIPTION, strategy.current().definition().description())
                .set(STRATEGIES.PRIVACY, strategy.privacy().name())
                .set(STRATEGIES.VERSION_NUMBER, strategy.current().number())
                .set(STRATEGIES.CREATED_AT, now)
                .set(STRATEGIES.UPDATED_AT, now)
                .execute();
    }

    private static SortField<?> sortField(ListStrategiesQuery query) {
        Field<?> field =
                ListStrategiesQuery.SORT_STRATEGY_ID.equals(query.sort())
                        ? STRATEGIES.STRATEGY_ID
                        : STRATEGIES.NAME;
        if (ListStrategiesQuery.ORDER_DESC.equals(query.order())) {
            return field.desc();
        }
        return field.asc();
    }

    private static UUID uuid(StrategyId id) {
        return UUID.fromString(id.value());
    }

    private static String normalize(NormalizeText text) {
        return text.value().toLowerCase(Locale.ROOT);
    }

    private record NormalizeText(String value) {}

    private record VersionPatch(
            UUID strategyId,
            String privacy,
            int versionNumber,
            String description,
            OffsetDateTime updatedAt) {

        private static VersionPatch from(Strategy strategy) {
            return new VersionPatch(
                    uuid(strategy.id()),
                    strategy.privacy().name(),
                    strategy.current().number(),
                    strategy.current().definition().description(),
                    OffsetDateTime.now(ZoneOffset.UTC));
        }
    }
}
