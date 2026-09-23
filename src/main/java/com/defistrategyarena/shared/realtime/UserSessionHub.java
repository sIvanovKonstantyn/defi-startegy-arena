package com.defistrategyarena.shared.realtime;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UserSessionHub {

    void register(SessionRegistration registration);

    void unregister(SessionKey key);

    void push(PushCommand command);

    record SessionRegistration(String userId, SessionKey key, UserSessionSocket socket) {}

    record SessionKey(String value) {}

    record PushCommand(String userId, String payloadJson) {}
}
