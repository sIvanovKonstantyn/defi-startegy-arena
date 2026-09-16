package com.defistrategyarena.identity.domain;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.shared.kernel.IdempotentUuid;

public record UserId(String value) {

    private static final String MUST_NOT_BE_BLANK = "user id must not be blank";

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(MUST_NOT_BE_BLANK);
        }
    }

    public static UserId create(IdGenerationInput input) {
        return new UserId(IdempotentUuid.from(input).toString());
    }
}
