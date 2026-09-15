package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.Optional;

public interface StrategyRepository {

    void save(Strategy strategy);

    Optional<Strategy> get(StrategyId id);

    Optional<Strategy> findByOwnerAndName(OwnerStrategyName key);

    StrategyPage listByOwner(ListStrategiesQuery query);

    int size();

    int countByOwnerAndName(OwnerStrategyName key);
}
