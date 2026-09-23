package com.defistrategyarena.shared.realtime;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UserSessionSocket {

    void sendText(TextPayload payload);

    record TextPayload(String text) {}
}
