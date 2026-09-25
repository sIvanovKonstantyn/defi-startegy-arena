package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.strategy.adapter.persistence.StrategyJsonMaps;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategyPersistenceCodecTest {

    @Test
    void encodesAndDecodesMaps() {
        String json =
                StrategyJsonMaps.encode(
                        new StrategyJsonMaps.MapPayload(Map.of("period", "50", "instrument", "ETH-USD")));
        Map<String, String> decoded =
                StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload(json));
        assertEquals("50", decoded.get("period"));
        assertEquals("ETH-USD", decoded.get("instrument"));
    }

    @Test
    void decodeBlankAndNullJsonReturnsEmpty() {
        assertEquals(Map.of(), StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload("")));
        assertEquals(Map.of(), StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload("   ")));
        assertEquals(Map.of(), StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload(null)));
        assertEquals(Map.of(), StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload("null")));
    }

    @Test
    void mapPayloadNullValuesBecomesEmpty() {
        String json = StrategyJsonMaps.encode(new StrategyJsonMaps.MapPayload(null));
        assertEquals("{}", json);
    }

    @Test
    void nullPayloadRejected() {
        assertThrows(NullPointerException.class, () -> StrategyJsonMaps.encode(null));
        assertThrows(NullPointerException.class, () -> StrategyJsonMaps.decode(null));
    }

    @Test
    void decodeInvalidJsonFails() {
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> StrategyJsonMaps.decode(new StrategyJsonMaps.JsonPayload("{")));
        assertTrue(ex.getCause() instanceof JsonProcessingException);
    }
}
