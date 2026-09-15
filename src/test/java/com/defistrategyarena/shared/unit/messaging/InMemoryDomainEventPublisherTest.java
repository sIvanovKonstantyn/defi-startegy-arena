package com.defistrategyarena.shared.unit.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.events.strategy.StrategyVersionPublished;
import com.defistrategyarena.shared.messaging.InMemoryDomainEventPublisher;
import org.junit.jupiter.api.Test;

class InMemoryDomainEventPublisherTest {

    @Test
    void publishes_and_exposes_copy() {
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        StrategyVersionPublished event =
                new StrategyVersionPublished("id", 1, "owner");
        publisher.publish(event);
        assertEquals(1, publisher.published().size());
        assertEquals(event, publisher.published().getFirst());
    }

    @Test
    void rejects_null_event() {
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        assertThrows(NullPointerException.class, () -> publisher.publish(null));
    }
}
