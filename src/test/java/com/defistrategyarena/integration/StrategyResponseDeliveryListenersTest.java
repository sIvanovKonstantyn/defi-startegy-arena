package com.defistrategyarena.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.defistrategyarena.shared.messaging.DomainEventListenerRegistry;
import com.defistrategyarena.shared.realtime.UserSessionHub;
import com.defistrategyarena.shared.realtime.UserSessionSocket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyResponseDeliveryListenersTest {

    private static final String CORRELATION = "corr-1";
    private static final String OWNER = "owner-1";
    private static final String STRATEGY = "strategy-1";
    private static final String REASON = "BAD_REQUEST";
    private static final String KEY = "socket-1";
    private static final int VERSION = 2;
    private static final long TOTAL = 1L;
    private static final int EMPTY = 0;
    private static final int EXPECTED_MESSAGES = 10;

    @Test
    void all_response_events_are_pushed_as_envelopes() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        RecordingSocket socket = new RecordingSocket();
        hub.register(
                new UserSessionHub.SessionRegistration(
                        OWNER, new UserSessionHub.SessionKey(KEY), socket));
        DomainEventListenerRegistry registry = new DomainEventListenerRegistry();
        new StrategyResponseDeliveryListeners(
                        new StrategyResponseDeliveryListeners.StrategyResponseDeliveryListenersDeps(
                                hub, new ObjectMapper()))
                .register(registry);

        registry.dispatch(new CreateStrategyCompleted(CORRELATION, OWNER, STRATEGY));
        registry.dispatch(new CreateStrategyFailed(CORRELATION, OWNER, REASON));
        registry.dispatch(
                new ListStrategiesCompleted(CORRELATION, OWNER, List.of(), TOTAL));
        registry.dispatch(new ListStrategiesFailed(CORRELATION, OWNER, REASON));
        registry.dispatch(
                new GetStrategyCompleted(
                        CORRELATION, OWNER, STRATEGY, "alpha", "PRIVATE", VERSION, List.of()));
        registry.dispatch(new GetStrategyFailed(CORRELATION, OWNER, REASON));
        registry.dispatch(new UpdateStrategyCompleted(CORRELATION, OWNER, STRATEGY, VERSION));
        registry.dispatch(new UpdateStrategyFailed(CORRELATION, OWNER, REASON));
        registry.dispatch(new DeleteStrategyCompleted(CORRELATION, OWNER, STRATEGY));
        registry.dispatch(new DeleteStrategyFailed(CORRELATION, OWNER, REASON));

        assertEquals(EXPECTED_MESSAGES, socket.messages().size());
        assertTrue(socket.messages().get(EMPTY).contains("strategy.create"));
        assertTrue(socket.messages().get(EMPTY).contains(CORRELATION));
    }

    @Test
    void encode_failure_surfaces_as_illegal_state() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        ObjectMapper mapper =
                new ObjectMapper() {
                    @Override
                    public String writeValueAsString(Object value) throws JsonProcessingException {
                        throw new JsonProcessingException("boom") {};
                    }
                };
        DomainEventListenerRegistry registry = new DomainEventListenerRegistry();
        new StrategyResponseDeliveryListeners(
                        new StrategyResponseDeliveryListeners.StrategyResponseDeliveryListenersDeps(
                                hub, mapper))
                .register(registry);
        assertThrows(
                IllegalStateException.class,
                () -> registry.dispatch(new CreateStrategyCompleted(CORRELATION, OWNER, STRATEGY)));
    }

    private static final class RecordingSocket implements UserSessionSocket {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendText(TextPayload payload) {
            messages.add(payload.text());
        }

        List<String> messages() {
            return List.copyOf(messages);
        }
    }
}
