package com.defistrategyarena.shared.messaging;

import java.util.Optional;
import java.util.UUID;

public interface InboxStore {

    boolean tryClaim(InboxClaim claim);

    record InboxClaim(
            UUID eventId, String eventType, String payloadJson, Optional<String> correlationId) {}
}
