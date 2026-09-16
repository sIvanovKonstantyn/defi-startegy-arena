package com.defistrategyarena.strategy.adapter.persistence;

import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStrategyRepository implements StrategyRepository {

    private static final int EMPTY_COUNT = 0;
    private static final int SINGLE_COUNT = 1;
    private static final String UNKNOWN_STRATEGY = "strategy not found";

    private final Map<String, Strategy> byId = new ConcurrentHashMap<>();
    private final Map<String, String> ownerNameIndex = new ConcurrentHashMap<>();

    @Override
    public void save(Strategy strategy) {
        String indexKey =
                indexKey(new OwnerStrategyName(strategy.ownerId(), strategy.current().definition().name()));
        String existingId = ownerNameIndex.putIfAbsent(indexKey, strategy.id().value());
        if (existingId != null) {
            throw new DuplicateStrategyException();
        }
        byId.put(strategy.id().value(), strategy);
    }

    @Override
    public void update(Strategy strategy) {
        String id = strategy.id().value();
        if (!byId.containsKey(id)) {
            throw new IllegalArgumentException(UNKNOWN_STRATEGY);
        }
        byId.put(id, strategy);
    }

    @Override
    public void delete(StrategyId id) {
        Strategy removed = byId.remove(id.value());
        if (removed == null) {
            return;
        }
        ownerNameIndex.remove(
                indexKey(new OwnerStrategyName(removed.ownerId(), removed.current().definition().name())));
    }

    @Override
    public Optional<Strategy> get(StrategyId id) {
        return Optional.ofNullable(byId.get(id.value()));
    }

    @Override
    public Optional<Strategy> findByOwnerAndName(OwnerStrategyName key) {
        String strategyId = ownerNameIndex.get(indexKey(key));
        if (strategyId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(strategyId));
    }

    @Override
    public StrategyPage listByOwner(ListStrategiesQuery query) {
        List<Strategy> owned = new ArrayList<>();
        for (Strategy strategy : byId.values()) {
            if (query.ownerId().equals(strategy.ownerId())) {
                owned.add(strategy);
            }
        }
        owned.sort(comparator(query));
        long total = owned.size();
        int fromIndex = query.page() * query.size();
        if (fromIndex >= owned.size()) {
            return new StrategyPage(List.of(), total);
        }
        int toIndex = Math.min(fromIndex + query.size(), owned.size());
        return new StrategyPage(owned.subList(fromIndex, toIndex), total);
    }

    @Override
    public int size() {
        return byId.size();
    }

    @Override
    public int countByOwnerAndName(OwnerStrategyName key) {
        if (ownerNameIndex.containsKey(indexKey(key))) {
            return SINGLE_COUNT;
        }
        return EMPTY_COUNT;
    }

    private static Comparator<Strategy> comparator(ListStrategiesQuery query) {
        Comparator<Strategy> byField;
        if (ListStrategiesQuery.SORT_STRATEGY_ID.equals(query.sort())) {
            byField = Comparator.comparing(strategy -> strategy.id().value());
        } else {
            byField = Comparator.comparing(strategy -> strategy.current().definition().name());
        }
        if (ListStrategiesQuery.ORDER_DESC.equals(query.order())) {
            return byField.reversed();
        }
        return byField;
    }

    private static String indexKey(OwnerStrategyName key) {
        return key.ownerId().toLowerCase(Locale.ROOT) + '\u001f' + key.name().toLowerCase(Locale.ROOT);
    }
}
