package com.defistrategyarena.strategy.adapter.persistence;

import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.application.StrategyRepository;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStrategyRepository implements StrategyRepository {

    private static final int EMPTY_COUNT = 0;
    private static final int SINGLE_COUNT = 1;

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

    private static String indexKey(OwnerStrategyName key) {
        return key.ownerId().toLowerCase(Locale.ROOT) + '\u001f' + key.name().toLowerCase(Locale.ROOT);
    }
}
