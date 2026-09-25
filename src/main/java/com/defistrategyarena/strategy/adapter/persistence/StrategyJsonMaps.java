package com.defistrategyarena.strategy.adapter.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;

public final class StrategyJsonMaps {

    private static final String CODEC_REQUIRED = "json map input must not be null";
    private static final String ENCODE_FAILED = "failed to encode json map";
    private static final String DECODE_FAILED = "failed to decode json map";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private StrategyJsonMaps() {}

    public static String encode(MapPayload payload) {
        return encodeWith(new EncodeWithCommand(MAPPER, payload));
    }

    static String encodeWith(EncodeWithCommand command) {
        Objects.requireNonNull(command.payload(), CODEC_REQUIRED);
        try {
            return command.mapper().writeValueAsString(command.payload().values());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ENCODE_FAILED, ex);
        }
    }

    public static Map<String, String> decode(JsonPayload payload) {
        Objects.requireNonNull(payload, CODEC_REQUIRED);
        String json = payload.json();
        if (isBlankJson(new BlankJsonCheck(json))) {
            return Map.of();
        }
        return readMap(new JsonPayload(json));
    }

    private static boolean isBlankJson(BlankJsonCheck check) {
        return check.json() == null || check.json().isBlank();
    }

    private static Map<String, String> readMap(JsonPayload payload) {
        try {
            Map<String, String> values = MAPPER.readValue(payload.json(), MAP_TYPE);
            return values == null ? Map.of() : Map.copyOf(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(DECODE_FAILED, ex);
        }
    }

    public record MapPayload(Map<String, String> values) {
        public MapPayload {
            values = values == null ? Map.of() : Map.copyOf(values);
        }
    }

    public record JsonPayload(String json) {}

    record EncodeWithCommand(ObjectMapper mapper, MapPayload payload) {}

    private record BlankJsonCheck(String json) {}
}
