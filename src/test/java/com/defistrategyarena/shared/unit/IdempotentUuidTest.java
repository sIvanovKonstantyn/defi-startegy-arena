package com.defistrategyarena.shared.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import com.defistrategyarena.shared.kernel.IdempotentUuid;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotentUuidTest {

    @Test
    void same_fields_produce_same_uuid() {
        IdGenerationInput first =
                IdGenerationInput.create(
                        new IdGenerationInput.StringListFields(List.of("Owner", " alpha ")));
        IdGenerationInput second =
                IdGenerationInput.create(
                        new IdGenerationInput.StringListFields(List.of("owner", "alpha")));

        UUID left = IdempotentUuid.from(first);
        UUID right = IdempotentUuid.from(second);

        assertEquals(left, right);
    }

    @Test
    void different_fields_produce_different_uuid() {
        IdGenerationInput first =
                IdGenerationInput.create(new IdGenerationInput.StringListFields(List.of("a", "b")));
        IdGenerationInput second =
                IdGenerationInput.create(new IdGenerationInput.StringListFields(List.of("a", "c")));

        assertNotEquals(IdempotentUuid.from(first), IdempotentUuid.from(second));
    }

    @Test
    void rejects_null_input() {
        assertThrows(NullPointerException.class, () -> IdempotentUuid.from(null));
    }

    @Test
    void enum_catalog_helpers_are_reachable() {
        assertEquals(0, IdempotentUuid.values().length);
        assertThrows(IllegalArgumentException.class, () -> IdempotentUuid.valueOf("missing"));
    }
}
