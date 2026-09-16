package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.List;

public record ListStrategiesCompleted(
        String correlationId, List<StrategySummaryPayload> items, long total)
        implements DomainEvent {

    public ListStrategiesCompleted {
        items = List.copyOf(items);
    }

    public record StrategySummaryPayload(
            String strategyId, String name, String privacy, int versionNumber) {}
}
