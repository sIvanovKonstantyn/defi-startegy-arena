package com.defistrategyarena.strategy.adapter.persistence;

import com.defistrategyarena.strategy.adapter.dsl.StrategyDslWireMapper;
import com.defistrategyarena.strategy.adapter.dsl.StrategyRuleWire;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StrategyDefinitionJsonCodec {

    private static final String CODEC_REQUIRED = "codec input must not be null";
    private static final String ENCODE_FAILED = "failed to encode strategy definition";
    private static final String DECODE_FAILED = "failed to decode strategy definition";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StrategyDefinitionJsonCodec() {}

    public static String encode(StrategyDefinition definition) {
        Objects.requireNonNull(definition, CODEC_REQUIRED);
        List<StrategyRuleWire> rules = new ArrayList<>();
        for (StrategyDefinition.Rule rule : definition.rules()) {
            rules.add(StrategyDslWireMapper.toWire(rule));
        }
        return writeJson(
                new JsonWriteRequest(MAPPER, new DefinitionWire(definition.name(), List.copyOf(rules)), ENCODE_FAILED));
    }

    public static StrategyDefinition decode(JsonPayload payload) {
        Objects.requireNonNull(payload, CODEC_REQUIRED);
        try {
            DefinitionWire wire = MAPPER.readValue(payload.json(), DefinitionWire.class);
            List<StrategyDefinition.Rule> rules = new ArrayList<>();
            for (StrategyRuleWire rule : wire.rules()) {
                rules.add(StrategyDslWireMapper.toRule(rule));
            }
            return StrategyDefinition.create(new StrategyDefinition(wire.name(), List.copyOf(rules)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(DECODE_FAILED, ex);
        }
    }

    static String writeJson(JsonWriteRequest request) {
        try {
            return request.mapper().writeValueAsString(request.value());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(request.failureMessage(), ex);
        }
    }

    public record JsonPayload(String json) {}

    record JsonWriteRequest(ObjectMapper mapper, Object value, String failureMessage) {}

    private record DefinitionWire(String name, List<StrategyRuleWire> rules) {}
}
