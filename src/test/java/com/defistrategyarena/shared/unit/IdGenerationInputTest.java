package com.defistrategyarena.shared.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.shared.kernel.IdGenerationInput;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdGenerationInputTest {

    @Test
    void creates_defensive_copy_of_fields() {
        List<String> mutable = new ArrayList<>();
        mutable.add("one");
        IdGenerationInput input =
                IdGenerationInput.create(new IdGenerationInput.StringListFields(mutable));
        mutable.add("two");
        assertEquals(List.of("one"), input.fields());
    }

    @Test
    void rejects_null_field_list() {
        assertThrows(NullPointerException.class, () -> new IdGenerationInput(null));
    }

    @Test
    void rejects_empty_field_list() {
        assertThrows(IllegalArgumentException.class, () -> new IdGenerationInput(List.of()));
    }

    @Test
    void rejects_null_field_value() {
        List<String> fields = new ArrayList<>();
        fields.add(null);
        assertThrows(NullPointerException.class, () -> new IdGenerationInput(fields));
    }

    @Test
    void rejects_null_string_list_fields_values() {
        assertThrows(
                NullPointerException.class, () -> new IdGenerationInput.StringListFields(null));
    }
}
