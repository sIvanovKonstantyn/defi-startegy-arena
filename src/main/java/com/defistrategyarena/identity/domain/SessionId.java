package com.defistrategyarena.identity.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.shared.kernel.IdempotentUuid;

public record SessionId(String value) {

    private static final String MUST_NOT_BE_BLANK = "session id must not be blank";

    public SessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public static SessionId create(IdGenerationInput input) {
        return new SessionId(IdempotentUuid.from(input).toString());
    }
}
