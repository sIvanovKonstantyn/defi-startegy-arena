package com.defistrategyarena.shared.unit.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.integration.InMemoryUserSessionHub;
import com.defistrategyarena.shared.infra.http.WebSocketAccessTokenAuth;
import com.defistrategyarena.shared.infra.http.WebSocketBinding;
import com.defistrategyarena.shared.infra.http.jetty.JettyUserSessionWebSocket;
import com.defistrategyarena.shared.realtime.UserSessionHub;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;

class JettyUserSessionWebSocketTest {

    private static final String USER_ID = "user-1";
    private static final String TOKEN = "valid-token";
    private static final String AUTH_OK = "{\"type\":\"auth\",\"status\":\"ok\"}";
    private static final String AUTH_FAILED = "{\"type\":\"auth\",\"status\":\"failed\"}";
    private static final String AUTH_FRAME =
            "{\"type\":\"auth\",\"accessToken\":\"" + TOKEN + "\"}";
    private static final String BAD_TYPE = "{\"type\":\"ping\",\"accessToken\":\"" + TOKEN + "\"}";
    private static final String BLANK_TOKEN = "{\"type\":\"auth\",\"accessToken\":\"   \"}";
    private static final String INVALID_JSON = "{not-json";
    private static final String PAYLOAD = "{\"hello\":true}";
    private static final int EMPTY = 0;
    private static final int SINGLE = 1;
    private static final int CLOSE_CODE = 1000;
    private static final String CLOSE_REASON = "bye";
    private static final String SEND_TEXT = "sendText";
    private static final String IS_OPEN = "isOpen";
    private static final String CLOSE = "close";

    @Test
    void authenticates_registers_and_delivers_push() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        WebSocketAccessTokenAuth auth =
                command -> Optional.of(new WebSocketAccessTokenAuth.WebSocketPrincipal(USER_ID));
        JettyUserSessionWebSocket socket =
                new JettyUserSessionWebSocket(new WebSocketBinding(auth, hub));
        FakeSession session = FakeSession.open();
        socket.onOpen(session.proxy());
        socket.onMessage(AUTH_FRAME);
        assertEquals(List.of(AUTH_OK), session.texts());
        assertEquals(SINGLE, hub.size());
        hub.push(new UserSessionHub.PushCommand(USER_ID, PAYLOAD));
        assertEquals(List.of(AUTH_OK, PAYLOAD), session.texts());
        socket.onMessage(AUTH_FRAME);
        assertEquals(List.of(AUTH_OK, PAYLOAD), session.texts());
    }

    @Test
    void rejects_invalid_auth_frames() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        WebSocketAccessTokenAuth auth = command -> Optional.empty();
        JettyUserSessionWebSocket socket =
                new JettyUserSessionWebSocket(new WebSocketBinding(auth, hub));
        FakeSession session = FakeSession.open();
        socket.onOpen(session.proxy());
        socket.onMessage(INVALID_JSON);
        assertEquals(List.of(AUTH_FAILED), session.texts());
        assertTrue(session.closed());
        assertEquals(EMPTY, hub.size());
    }

    @Test
    void rejects_wrong_type_blank_token_and_failed_principal() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        WebSocketAccessTokenAuth auth = command -> Optional.empty();
        JettyUserSessionWebSocket socket =
                new JettyUserSessionWebSocket(new WebSocketBinding(auth, hub));
        FakeSession session = FakeSession.open();
        socket.onOpen(session.proxy());
        socket.onMessage(BAD_TYPE);
        socket.onMessage(BLANK_TOKEN);
        socket.onMessage(AUTH_FRAME);
        assertEquals(List.of(AUTH_FAILED, AUTH_FAILED, AUTH_FAILED), session.texts());
        assertTrue(session.closed());
        assertEquals(EMPTY, hub.size());
    }

    @Test
    void ignores_message_before_open_and_unregisters_on_close_error() {
        InMemoryUserSessionHub hub = new InMemoryUserSessionHub();
        WebSocketAccessTokenAuth auth =
                command -> Optional.of(new WebSocketAccessTokenAuth.WebSocketPrincipal(USER_ID));
        JettyUserSessionWebSocket socket =
                new JettyUserSessionWebSocket(new WebSocketBinding(auth, hub));
        socket.onMessage(AUTH_FRAME);
        assertEquals(EMPTY, hub.size());
        FakeSession session = FakeSession.closedSession();
        socket.onOpen(session.proxy());
        socket.onMessage(AUTH_FRAME);
        assertEquals(SINGLE, hub.size());
        hub.push(new UserSessionHub.PushCommand(USER_ID, PAYLOAD));
        assertEquals(List.of(AUTH_OK), session.texts());
        socket.onClose(CLOSE_CODE, CLOSE_REASON);
        assertEquals(EMPTY, hub.size());
        socket.onOpen(session.proxy());
        socket.onMessage(AUTH_FRAME);
        assertEquals(SINGLE, hub.size());
        socket.onError(new IllegalStateException(CLOSE_REASON));
        assertEquals(EMPTY, hub.size());
    }

    @Test
    void bind_requires_non_null_binding() {
        try {
            new JettyUserSessionWebSocket(null);
        } catch (NullPointerException exception) {
            assertTrue(exception.getMessage().contains("websocket binding"));
        }
    }

    private static final class FakeSession implements InvocationHandler {
        private final List<String> texts = new ArrayList<>();
        private final AtomicBoolean open;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private final Session proxy;

        private FakeSession(boolean initiallyOpen) {
            this.open = new AtomicBoolean(initiallyOpen);
            this.proxy =
                    (Session)
                            Proxy.newProxyInstance(
                                    Session.class.getClassLoader(),
                                    new Class<?>[] {Session.class},
                                    this);
        }

        static FakeSession open() {
            return new FakeSession(true);
        }

        static FakeSession closedSession() {
            return new FakeSession(false);
        }

        Session proxy() {
            return proxy;
        }

        List<String> texts() {
            return List.copyOf(texts);
        }

        boolean closed() {
            return closed.get();
        }

        @Override
        public Object invoke(Object proxyInstance, Method method, Object[] args) {
            String name = method.getName();
            if (SEND_TEXT.equals(name)) {
                texts.add((String) args[0]);
                return null;
            }
            if (IS_OPEN.equals(name)) {
                return open.get();
            }
            if (CLOSE.equals(name)) {
                closed.set(true);
                open.set(false);
                return null;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType.equals(boolean.class)) {
                return false;
            }
            if (returnType.equals(int.class) || returnType.equals(long.class)) {
                return EMPTY;
            }
            return null;
        }
    }
}
