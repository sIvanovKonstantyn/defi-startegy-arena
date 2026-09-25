package com.defistrategyarena.shared.indicators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class IndicatorCatalogTest {

    private static final int EXPECTED_COUNT = 5;

    @Test
    void allReturnsSeededIndicators() {
        assertEquals(EXPECTED_COUNT, IndicatorCatalog.all().size());
        assertTrue(IndicatorCatalog.find(new IndicatorId("sma")).isPresent());
    }

    @Test
    void requireUnknownThrows() {
        assertThrows(
                IllegalArgumentException.class,
                () -> IndicatorCatalog.require(new IndicatorId("nope")));
    }

    @Test
    void validateParametersRequiresPeriodForSma() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        IndicatorCatalog.validateParameters(
                                new IndicatorCatalog.ValidateParametersCommand(
                                        new IndicatorId("sma"), Map.of())));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        IndicatorCatalog.validateParameters(
                                new IndicatorCatalog.ValidateParametersCommand(
                                        new IndicatorId("sma"), Map.of("period", " "))));
        IndicatorCatalog.validateParameters(
                new IndicatorCatalog.ValidateParametersCommand(
                        new IndicatorId("sma"), Map.of("period", "50")));
    }
}
