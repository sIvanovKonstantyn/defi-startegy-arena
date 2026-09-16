package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.strategy.adapter.persistence.StrategyDefinitionJsonCodec;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyPersistenceCodecTest {

    private static final String NAME = "codec";
    private static final String INSTRUMENT = "ETH-USD";
    private static final String THRESHOLD = "10";
    private static final String ALLOCATION = "5";
    private static final String INDICATOR = "RSI";
    private static final int VERSION = 3;

    @Test
    void encode_decode_round_trips_all_rule_shapes() {
        StrategyDefinition definition =
                new StrategyDefinition(
                        NAME,
                        List.of(
                                new StrategyDefinition.Rule(
                                        "a",
                                        new StrategyDefinition.PriceAbove(INSTRUMENT, THRESHOLD),
                                        new StrategyDefinition.Hold()),
                                new StrategyDefinition.Rule(
                                        "b",
                                        new StrategyDefinition.PriceUnder(INSTRUMENT, THRESHOLD),
                                        new StrategyDefinition.Buy(INSTRUMENT, ALLOCATION)),
                                new StrategyDefinition.Rule(
                                        "c",
                                        new StrategyDefinition.IndicatorBelow(INDICATOR, THRESHOLD),
                                        new StrategyDefinition.Sell(INSTRUMENT, ALLOCATION)),
                                new StrategyDefinition.Rule(
                                        "d",
                                        new StrategyDefinition.IndicatorAbove(INDICATOR, THRESHOLD),
                                        new StrategyDefinition.Hold())));
        String json = StrategyDefinitionJsonCodec.encode(definition);
        StrategyDefinition decoded =
                StrategyDefinitionJsonCodec.decode(new StrategyDefinitionJsonCodec.JsonPayload(json));
        assertEquals(definition, decoded);
    }

    @Test
    void decode_rejects_unknown_condition() {
        String json =
                "{\"name\":\"n\",\"rules\":[{\"id\":\"r\",\"conditionType\":\"nope\",\"actionType\":\"hold\",\"instrument\":\"\",\"indicator\":\"\",\"threshold\":\"\",\"allocationPercent\":\"\"}]}";
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        StrategyDefinitionJsonCodec.decode(
                                new StrategyDefinitionJsonCodec.JsonPayload(json)));
    }

    @Test
    void decode_rejects_unknown_action() {
        String json =
                "{\"name\":\"n\",\"rules\":[{\"id\":\"r\",\"conditionType\":\"price_above\",\"actionType\":\"nope\",\"instrument\":\"ETH\",\"indicator\":\"\",\"threshold\":\"1\",\"allocationPercent\":\"\"}]}";
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        StrategyDefinitionJsonCodec.decode(
                                new StrategyDefinitionJsonCodec.JsonPayload(json)));
    }

    @Test
    void decode_rejects_invalid_json() {
        assertThrows(
                IllegalStateException.class,
                () ->
                        StrategyDefinitionJsonCodec.decode(
                                new StrategyDefinitionJsonCodec.JsonPayload("{")));
    }

    @Test
    void buy_without_instrument_rejected_on_decode() {
        String json =
                "{\"name\":\"n\",\"rules\":[{\"id\":\"r\",\"conditionType\":\"price_above\",\"actionType\":\"buy\",\"instrument\":\"\",\"indicator\":\"\",\"threshold\":\"1\",\"allocationPercent\":\"5\"}]}";
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        StrategyDefinitionJsonCodec.decode(
                                new StrategyDefinitionJsonCodec.JsonPayload(json)));
    }

    @Test
    void sell_without_allocation_rejected_on_decode() {
        String json =
                "{\"name\":\"n\",\"rules\":[{\"id\":\"r\",\"conditionType\":\"price_above\",\"actionType\":\"sell\",\"instrument\":\"ETH\",\"indicator\":\"\",\"threshold\":\"1\",\"allocationPercent\":\"\"}]}";
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        StrategyDefinitionJsonCodec.decode(
                                new StrategyDefinitionJsonCodec.JsonPayload(json)));
    }

    @Test
    void rehydrate_rebuilds_aggregate() {
        StrategyDefinition definition = new StrategyDefinition(NAME, List.of());
        Strategy strategy =
                Strategy.rehydrate(
                        new Strategy.RehydrateData(
                                new StrategyId("00000000-0000-0000-0000-000000000001"),
                                "owner",
                                Privacy.SHARED,
                                VERSION,
                                definition));
        assertEquals(VERSION, strategy.current().number());
        assertEquals(Privacy.SHARED, strategy.privacy());
        assertEquals(NAME, strategy.current().definition().name());
    }

    @Test
    void rehydrate_rejects_blank_owner() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        Strategy.rehydrate(
                                new Strategy.RehydrateData(
                                        new StrategyId("00000000-0000-0000-0000-000000000001"),
                                        " ",
                                        Privacy.PRIVATE,
                                        VERSION,
                                        new StrategyDefinition(NAME, List.of()))));
    }

    @Test
    void rehydrate_rejects_null_data() {
        assertThrows(NullPointerException.class, () -> Strategy.rehydrate(null));
    }

    @Test
    void duplicate_exception_keeps_cause() {
        RuntimeException cause = new RuntimeException("db");
        DuplicateStrategyException ex = new DuplicateStrategyException(cause);
        assertEquals(cause, ex.getCause());
    }
}
