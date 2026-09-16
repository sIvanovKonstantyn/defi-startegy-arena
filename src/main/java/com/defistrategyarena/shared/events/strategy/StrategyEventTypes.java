package com.defistrategyarena.shared.events.strategy;

import com.defistrategyarena.shared.messaging.DomainEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public enum StrategyEventTypes {
    ;

    public static Map<String, Class<? extends DomainEvent>> catalog() {
        Map<String, Class<? extends DomainEvent>> types = new LinkedHashMap<>();
        put(new CatalogPut(types, StrategyVersionPublished.class));
        put(new CatalogPut(types, CreateStrategyRequested.class));
        put(new CatalogPut(types, CreateStrategyCompleted.class));
        put(new CatalogPut(types, CreateStrategyFailed.class));
        put(new CatalogPut(types, ListStrategiesRequested.class));
        put(new CatalogPut(types, ListStrategiesCompleted.class));
        put(new CatalogPut(types, ListStrategiesFailed.class));
        put(new CatalogPut(types, GetStrategyRequested.class));
        put(new CatalogPut(types, GetStrategyCompleted.class));
        put(new CatalogPut(types, GetStrategyFailed.class));
        put(new CatalogPut(types, UpdateStrategyRequested.class));
        put(new CatalogPut(types, UpdateStrategyCompleted.class));
        put(new CatalogPut(types, UpdateStrategyFailed.class));
        put(new CatalogPut(types, DeleteStrategyRequested.class));
        put(new CatalogPut(types, DeleteStrategyCompleted.class));
        put(new CatalogPut(types, DeleteStrategyFailed.class));
        return Map.copyOf(types);
    }

    private static void put(CatalogPut command) {
        command.types().put(command.type().getName(), command.type());
    }

    private record CatalogPut(
            Map<String, Class<? extends DomainEvent>> types, Class<? extends DomainEvent> type) {}
}
