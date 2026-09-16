package com.defistrategyarena.shared.messaging;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryInboxStore implements InboxStore {

    private static final String CLAIM_REQUIRED = "inbox claim must not be null";
    private static final Object PRESENT = new Object();

    private final Map<UUID, Object> claimed = new ConcurrentHashMap<>();

    @Override
    public boolean tryClaim(InboxClaim claim) {
        Objects.requireNonNull(claim, CLAIM_REQUIRED);
        return claimed.putIfAbsent(claim.eventId(), PRESENT) == null;
    }

    public int size() {
        return claimed.size();
    }
}
