package com.defistrategyarena.strategy.adapter.dsl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StrategyDslWireNamesTest {

    @Test
    void wire_name_constants_are_loaded() {
        assertTrue(StrategyDslWireNames.CONDITION_PRICE_ABOVE.contains("price"));
        assertTrue(StrategyDslWireNames.ACTION_HOLD.contains("hold"));
    }
}
