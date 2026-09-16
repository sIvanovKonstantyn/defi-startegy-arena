package com.defistrategyarena.shared.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.messaging.DomainEvent;
import com.defistrategyarena.shared.messaging.DomainEventCodec;
import com.defistrategyarena.shared.events.strategy.StrategyEventTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class JacksonDomainEventCodecTest {

    private static final int VERSION = 7;

    @Test
    void non_string_correlation_id_is_treated_as_absent() throws Exception {
        JacksonDomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        NonStringCorrelationEvent event = new NonStringCorrelationEvent(VERSION);
        Method accessor = event.getClass().getMethod("correlationId");
        assertInstanceOf(Integer.class, accessor.invoke(event));
        DomainEventCodec.EncodedEvent encoded = codec.encode(event);
        assertTrue(encoded.correlationId().isEmpty());
    }

    @Test
    void null_correlation_id_is_treated_as_absent() {
        JacksonDomainEventCodec codec =
                new JacksonDomainEventCodec(
                        new JacksonDomainEventCodec.JacksonDomainEventCodecDeps(
                                new ObjectMapper(), StrategyEventTypes.catalog()));
        DomainEventCodec.EncodedEvent encoded = codec.encode(new NullStringCorrelationEvent(null));
        assertTrue(encoded.correlationId().isEmpty());
    }

    private record NonStringCorrelationEvent(Integer correlationId) implements DomainEvent {}

    private record NullStringCorrelationEvent(String correlationId) implements DomainEvent {}
}
