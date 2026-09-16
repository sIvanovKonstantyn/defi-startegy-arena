package com.defistrategyarena.identity.application;

import com.defistrategyarena.identity.domain.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(PlainPassword password);

    boolean matches(PasswordMatchRequest request);

    record PlainPassword(String value) {}

    record PasswordMatchRequest(PlainPassword password, PasswordHash hash) {}
}
