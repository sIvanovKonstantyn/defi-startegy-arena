package com.defistrategyarena.strategy.adapter.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrategyDslWireNamesTest {

    @Test
    void enum_constants_are_loaded() {
        assertEquals(0, StrategyDslWireNames.values().length);
        assertTrue(StrategyDslWireNames.CONDITION_PRICE_ABOVE.contains("price"));
    }
}
