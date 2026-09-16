package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Optional;

enum OwnedStrategyLookup {
    ;

    static Optional<Strategy> find(OwnedStrategyLookupQuery query) {
        Optional<Strategy> found = query.strategies().get(query.strategyId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Strategy existing = found.get();
        if (!query.ownerId().equals(existing.ownerId())) {
            return Optional.empty();
        }
        return Optional.of(existing);
    }

    record OwnedStrategyLookupQuery(StrategyRepository strategies, String ownerId, StrategyId strategyId) {}
}
