package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.WebSocketAccessTokenAuth;
import com.defistrategyarena.shared.infra.http.WebSocketBinding;
import com.defistrategyarena.shared.realtime.UserSessionHub;
import com.defistrategyarena.shared.realtime.UserSessionSocket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@WebSocket
@SuppressWarnings("PMD.MethodsTakeAtMostOneDtoParameter")
public final class JettyUserSessionWebSocket {

    private static final String BINDING_REQUIRED = "websocket binding must not be null";
    private static final String TYPE_FIELD = "type";
    private static final String TOKEN_FIELD = "accessToken";
    private static final String AUTH_TYPE = "auth";
    private static final String AUTH_OK = "{\"type\":\"auth\",\"status\":\"ok\"}";
    private static final String AUTH_FAILED = "{\"type\":\"auth\",\"status\":\"failed\"}";

    private final WebSocketAccessTokenAuth auth;
    private final UserSessionHub hub;
    private final ObjectMapper objectMapper;
    private final UserSessionHub.SessionKey sessionKey;

    private @Nullable Session session;
    private boolean authenticated;

    public JettyUserSessionWebSocket(WebSocketBinding binding) {
        Objects.requireNonNull(binding, BINDING_REQUIRED);
        this.auth = binding.auth();
        this.hub = binding.hub();
        this.objectMapper = new ObjectMapper();
        this.sessionKey = new UserSessionHub.SessionKey(UUID.randomUUID().toString());
        this.authenticated = false;
    }

    @OnWebSocketOpen
    public void onOpen(Session opened) {
        this.session = opened;
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        handleAuthMessage(new IncomingText(message));
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        handleClose(new CloseSignal(statusCode, reason));
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        hub.unregister(sessionKey);
    }

    @SuppressWarnings("PMD.CloseResource")
    private void handleAuthMessage(IncomingText incoming) {
        if (authenticated) {
            return;
        }
        Session active = session;
        if (active == null) {
            return;
        }
        completeAuth(new AuthAttempt(active, incoming));
    }

    private void completeAuth(AuthAttempt attempt) {
        Optional<String> token = readAuthToken(attempt.incoming());
        if (token.isEmpty()) {
            reject(attempt.session());
            return;
        }
        Optional<WebSocketAccessTokenAuth.WebSocketPrincipal> principal =
                auth.authenticate(new WebSocketAccessTokenAuth.AccessTokenCommand(token.get()));
        if (principal.isEmpty()) {
            reject(attempt.session());
            return;
        }
        accept(new AcceptedAuth(attempt.session(), principal.get().userId()));
    }

    private void accept(AcceptedAuth accepted) {
        hub.register(
                new UserSessionHub.SessionRegistration(
                        accepted.userId(),
                        sessionKey,
                        new JettySessionSocket(accepted.session())));
        authenticated = true;
        accepted.session().sendText(AUTH_OK, null);
    }

    private void handleClose(CloseSignal signal) {
        Objects.requireNonNull(signal);
        hub.unregister(sessionKey);
        session = null;
        authenticated = false;
    }

    private void reject(Session active) {
        active.sendText(AUTH_FAILED, null);
        active.close();
    }

    private Optional<String> readAuthToken(IncomingText incoming) {
        try {
            JsonNode root = objectMapper.readTree(incoming.text());
            return extractToken(root);
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private static Optional<String> extractToken(JsonNode root) {
        if (!AUTH_TYPE.equals(root.path(TYPE_FIELD).asText())) {
            return Optional.empty();
        }
        String token = root.path(TOKEN_FIELD).asText();
        if (token.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    private record IncomingText(String text) {}

    private record CloseSignal(int statusCode, String reason) {}

    private record AuthAttempt(Session session, IncomingText incoming) {}

    private record AcceptedAuth(Session session, String userId) {}

    private static final class JettySessionSocket implements UserSessionSocket {

        private final Session session;

        private JettySessionSocket(Session session) {
            this.session = session;
        }

        @Override
        public void sendText(TextPayload payload) {
            if (session.isOpen()) {
                session.sendText(payload.text(), null);
            }
        }
    }
}
