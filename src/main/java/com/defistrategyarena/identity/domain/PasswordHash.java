package com.defistrategyarena.identity.domain;

import java.util.Objects;

public record PasswordHash(String encoded) {

    private static final String VALUE_REQUIRED = "password hash must not be blank";
    private static final String DRAFT_REQUIRED = "password hash draft must not be null";

    public PasswordHash {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalArgumentException(VALUE_REQUIRED);
        }
    }

    public static PasswordHash create(PasswordHash draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new PasswordHash(draft.encoded());
    }
}
