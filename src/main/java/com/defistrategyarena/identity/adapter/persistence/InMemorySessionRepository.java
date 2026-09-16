package com.defistrategyarena.identity.adapter.persistence;

import com.defistrategyarena.identity.application.SessionRepository;
import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.SessionId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySessionRepository implements SessionRepository {

    private final Map<String, Session> byId = new ConcurrentHashMap<>();
    private final Map<String, String> tokenHashIndex = new ConcurrentHashMap<>();

    @Override
    public void save(Session session) {
        byId.put(session.id().value(), session);
        tokenHashIndex.put(session.tokenHash(), session.id().value());
    }

    @Override
    public void delete(SessionId id) {
        Session removed = byId.remove(id.value());
        if (removed == null) {
            return;
        }
        tokenHashIndex.remove(removed.tokenHash());
    }

    @Override
    public Optional<Session> findByTokenHash(TokenHashLookup lookup) {
        String sessionId = tokenHashIndex.get(lookup.tokenHash());
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(sessionId));
    }
}
