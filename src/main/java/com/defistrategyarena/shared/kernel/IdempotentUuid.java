package com.defistrategyarena.shared.kernel;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public enum IdempotentUuid {
    ;

    private static final String INPUT_REQUIRED = "id generation input must not be null";
    private static final String FIELD_SEPARATOR = "\u001f";
    private static final UUID NAMESPACE =
            UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    public static UUID from(IdGenerationInput input) {
        Objects.requireNonNull(input, INPUT_REQUIRED);
        String canonical = canonicalize(input);
        String namespaced = NAMESPACE + FIELD_SEPARATOR + canonical;
        return UUID.nameUUIDFromBytes(namespaced.getBytes(StandardCharsets.UTF_8));
    }

    private static String canonicalize(IdGenerationInput input) {
        return input.fields().stream()
                .map(field -> field.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(FIELD_SEPARATOR));
    }
}
