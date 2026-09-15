package com.defistrategyarena.shared.kernel;

import java.util.List;
import java.util.Objects;

public record IdGenerationInput(List<String> fields) {

    private static final String FIELDS_REQUIRED = "id generation fields must not be null";
    private static final String FIELDS_MUST_NOT_BE_EMPTY = "id generation fields must not be empty";
    private static final String FIELD_REQUIRED = "id generation field must not be null";

    public IdGenerationInput {
        Objects.requireNonNull(fields, FIELDS_REQUIRED);
        if (fields.isEmpty()) {
            throw new IllegalArgumentException(FIELDS_MUST_NOT_BE_EMPTY);
        }
        for (String field : fields) {
            Objects.requireNonNull(field, FIELD_REQUIRED);
        }
        fields = List.copyOf(fields);
    }

    public static IdGenerationInput create(StringListFields fields) {
        return new IdGenerationInput(fields.values());
    }

    public record StringListFields(List<String> values) {
        public StringListFields {
            Objects.requireNonNull(values, FIELDS_REQUIRED);
            values = List.copyOf(values);
        }
    }
}
