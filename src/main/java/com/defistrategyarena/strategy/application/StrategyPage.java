package com.defistrategyarena.strategy.application;

import com.defistrategyarena.strategy.domain.Strategy;
import java.util.List;

public record StrategyPage(List<Strategy> items, long totalElements) {

    private static final String ITEMS_REQUIRED = "items must not be null";

    public StrategyPage {
        if (items == null) {
            throw new IllegalArgumentException(ITEMS_REQUIRED);
        }
        items = List.copyOf(items);
    }

    public static StrategyPage create(StrategyPage draft) {
        return new StrategyPage(draft.items(), draft.totalElements());
    }
}
