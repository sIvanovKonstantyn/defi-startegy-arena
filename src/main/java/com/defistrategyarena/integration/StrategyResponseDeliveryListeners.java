package com.defistrategyarena.integration;

import com.defistrategyarena.shared.events.strategy.CreateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.CreateStrategyFailed;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.DeleteStrategyFailed;
import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.GetStrategyFailed;
import com.defistrategyarena.shared.events.strategy.ListStrategiesCompleted;
import com.defistrategyarena.shared.events.strategy.ListStrategiesFailed;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyFailed;
import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.realtime.UserSessionHub;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class StrategyResponseDeliveryListeners {

    private static final String DEPS_REQUIRED = "delivery listeners deps must not be null";
    private static final String ENCODE_FAILED = "failed to encode websocket envelope";

    private final UserSessionHub hub;
    private final ObjectMapper objectMapper;
    private final StrategyResponseEnvelopes envelopes;

    public StrategyResponseDeliveryListeners(StrategyResponseDeliveryListenersDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.hub = deps.hub();
        this.objectMapper = deps.objectMapper();
        this.envelopes = new StrategyResponseEnvelopes();
    }

    public void register(DomainEventListenerRegistry registry) {
        bind(new BindSpec<>(registry, CreateStrategyCompleted.class, envelopes::createCompleted));
        bind(new BindSpec<>(registry, CreateStrategyFailed.class, envelopes::createFailed));
        bind(new BindSpec<>(registry, ListStrategiesCompleted.class, envelopes::listCompleted));
        bind(new BindSpec<>(registry, ListStrategiesFailed.class, envelopes::listFailed));
        bind(new BindSpec<>(registry, GetStrategyCompleted.class, envelopes::getCompleted));
        bind(new BindSpec<>(registry, GetStrategyFailed.class, envelopes::getFailed));
        bind(new BindSpec<>(registry, UpdateStrategyCompleted.class, envelopes::updateCompleted));
        bind(new BindSpec<>(registry, UpdateStrategyFailed.class, envelopes::updateFailed));
        bind(new BindSpec<>(registry, DeleteStrategyCompleted.class, envelopes::deleteCompleted));
        bind(new BindSpec<>(registry, DeleteStrategyFailed.class, envelopes::deleteFailed));
    }

    private <E extends DomainEvent> void bind(BindSpec<E> spec) {
        spec.registry()
                .register(
                        new DomainEventListenerRegistry.ListenerRegistration<>(
                                spec.type(), event -> push(spec.mapper().apply(event))));
    }

    private void push(StrategyResponseEnvelopes.PushEnvelope command) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put(StrategyResponseEnvelopes.KEY_CORRELATION, command.correlationId());
        envelope.put(StrategyResponseEnvelopes.KEY_TYPE, command.type());
        envelope.put(StrategyResponseEnvelopes.KEY_STATUS, command.status());
        envelope.put(StrategyResponseEnvelopes.KEY_PAYLOAD, command.payload());
        try {
            hub.push(
                    new UserSessionHub.PushCommand(
                            command.ownerId(), objectMapper.writeValueAsString(envelope)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(ENCODE_FAILED, exception);
        }
    }

    public record StrategyResponseDeliveryListenersDeps(
            UserSessionHub hub, ObjectMapper objectMapper) {}

    private record BindSpec<E extends DomainEvent>(
            DomainEventListenerRegistry registry,
            Class<E> type,
            Function<E, StrategyResponseEnvelopes.PushEnvelope> mapper) {}
}
