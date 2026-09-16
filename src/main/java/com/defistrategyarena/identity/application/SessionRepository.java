package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.Session;
import com.defistrategyarena.identity.domain.SessionId;
import java.util.Optional;

public interface SessionRepository {

    void save(Session session);

    void delete(SessionId id);

    Optional<Session> findByTokenHash(TokenHashLookup lookup);

    record TokenHashLookup(String tokenHash) {}
}
