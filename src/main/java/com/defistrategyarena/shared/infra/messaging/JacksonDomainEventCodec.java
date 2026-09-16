package com.defistrategyarena.shared.infra.messaging;

import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventCodec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JacksonDomainEventCodec implements DomainEventCodec {

    private static final String DEPS_REQUIRED = "codec deps must not be null";
    private static final String UNKNOWN_TYPE = "unknown domain event type: ";
    private static final String ENCODE_FAILED = "failed to encode domain event";
    private static final String DECODE_FAILED = "failed to decode domain event";
    private static final String CORRELATION_ACCESSOR = "correlationId";

    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends DomainEvent>> typesByName;

    public JacksonDomainEventCodec(JacksonDomainEventCodecDeps deps) {
        Objects.requireNonNull(deps, DEPS_REQUIRED);
        this.objectMapper = deps.objectMapper();
        this.typesByName = Map.copyOf(deps.typesByName());
    }

    @Override
    public EncodedEvent encode(DomainEvent event) {
        try {
            String type = event.getClass().getName();
            String payload = objectMapper.writeValueAsString(event);
            return new EncodedEvent(type, payload, correlationId(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(ENCODE_FAILED, exception);
        }
    }

    @Override
    public DomainEvent decode(EncodedEvent encoded) {
        Class<? extends DomainEvent> type = requireType(new EventTypeName(encoded.eventType()));
        try {
            return objectMapper.readValue(encoded.payloadJson(), type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(DECODE_FAILED, exception);
        }
    }

    private Class<? extends DomainEvent> requireType(EventTypeName eventType) {
        Class<? extends DomainEvent> type = typesByName.get(eventType.value());
        if (type == null) {
            throw new IllegalArgumentException(UNKNOWN_TYPE + eventType.value());
        }
        return type;
    }

    private static Optional<String> correlationId(DomainEvent event) {
        try {
            Method accessor = event.getClass().getMethod(CORRELATION_ACCESSOR);
            return textCorrelation(new CorrelationValue(accessor.invoke(event)));
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> textCorrelation(CorrelationValue value) {
        Object raw = value.raw();
        if (raw == null || raw.getClass() != String.class) {
            return Optional.empty();
        }
        String text = (String) raw;
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    private record EventTypeName(String value) {}

    private record CorrelationValue(Object raw) {}

    public record JacksonDomainEventCodecDeps(
            ObjectMapper objectMapper, Map<String, Class<? extends DomainEvent>> typesByName) {}
}
