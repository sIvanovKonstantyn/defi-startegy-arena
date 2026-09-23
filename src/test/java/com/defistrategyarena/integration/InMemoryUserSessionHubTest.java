package com.defistrategyarena.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.realtime.UserSessionHub;
import com.defistrategyarena.shared.realtime.UserSessionSocket;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryUserSessionHubTest {

    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";
    private static final String KEY_ONE = "k1";
    private static final String KEY_TWO = "k2";
    private static final String PAYLOAD = "{\"ok\":true}";
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
    private static final int TWO = 2;

    @Test
    void push_delivers_to_all_sockets_for_user() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        RecordingSocket first = new RecordingSocket();
        RecordingSocket second = new RecordingSocket();
        hub.register(
                new UserSessionHub.SessionRegistration(
                        USER_A, new UserSessionHub.SessionKey(KEY_ONE), first));
        hub.register(
                new UserSessionHub.SessionRegistration(
                        USER_A, new UserSessionHub.SessionKey(KEY_TWO), second));
        hub.push(new UserSessionHub.PushCommand(USER_A, PAYLOAD));
        assertEquals(List.of(PAYLOAD), first.messages());
        assertEquals(List.of(PAYLOAD), second.messages());
        assertEquals(TWO, hub.size());
        assertEquals(SINGLE, hub.userCount());
    }

    @Test
    void unregister_and_missing_push_are_safe() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        RecordingSocket socket = new RecordingSocket();
        hub.register(
                new UserSessionHub.SessionRegistration(
                        USER_A, new UserSessionHub.SessionKey(KEY_ONE), socket));
        hub.unregister(new UserSessionHub.SessionKey(KEY_ONE));
        hub.unregister(new UserSessionHub.SessionKey(KEY_TWO));
        hub.push(new UserSessionHub.PushCommand(USER_B, PAYLOAD));
        assertTrue(socket.messages().isEmpty());
        assertEquals(EMPTY, hub.size());
        assertEquals(EMPTY, hub.userCount());
    }

    @Test
    void push_ignores_other_users_sockets() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        RecordingSocket other = new RecordingSocket();
        RecordingSocket mine = new RecordingSocket();
        hub.register(
                new UserSessionHub.SessionRegistration(
                        USER_B, new UserSessionHub.SessionKey(KEY_ONE), other));
        hub.register(
                new UserSessionHub.SessionRegistration(
                        USER_A, new UserSessionHub.SessionKey(KEY_TWO), mine));
        hub.push(new UserSessionHub.PushCommand(USER_A, PAYLOAD));
        assertTrue(other.messages().isEmpty());
        assertEquals(List.of(PAYLOAD), mine.messages());
        assertEquals(TWO, hub.size());
        assertEquals(TWO, hub.userCount());
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
