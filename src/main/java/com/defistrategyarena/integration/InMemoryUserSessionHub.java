package com.defistrategyarena.integration;

import com.defistrategyarena.shared.realtime.UserSessionHub;
import com.defistrategyarena.shared.realtime.UserSessionSocket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class InMemoryUserSessionHub implements UserSessionHub {

    private static final String REGISTRATION_REQUIRED = "session registration must not be null";
    private static final String KEY_REQUIRED = "session key must not be null";
    private static final String PUSH_REQUIRED = "push command must not be null";

    private final Map<String, BoundSocket> sockets = new ConcurrentHashMap<>();

    @Override
    public void register(SessionRegistration registration) {
        Objects.requireNonNull(registration, REGISTRATION_REQUIRED);
        sockets.put(
                registration.key().value(),
                new BoundSocket(registration.userId(), registration.socket()));
    }

    @Override
    public void unregister(SessionKey key) {
        Objects.requireNonNull(key, KEY_REQUIRED);
        sockets.remove(key.value());
    }

    @Override
    public void push(PushCommand command) {
        Objects.requireNonNull(command, PUSH_REQUIRED);
        UserSessionSocket.TextPayload payload =
                new UserSessionSocket.TextPayload(command.payloadJson());
        String userId = command.userId();
        for (BoundSocket bound : sockets.values()) {
            if (userId.equals(bound.userId())) {
                bound.socket().sendText(payload);
            }
        }
    }

    public int size() {
        return sockets.size();
    }

    public int userCount() {
        return (int) sockets.values().stream().map(BoundSocket::userId).distinct().count();
    }

    private record BoundSocket(String userId, UserSessionSocket socket) {}
}
