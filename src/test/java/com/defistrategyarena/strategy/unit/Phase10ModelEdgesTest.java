package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.shared.events.strategy.CreateStrategyRequested;
import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.UpdateStrategyRequested;
import com.defistrategyarena.shared.indicators.IndicatorCatalog;
import com.defistrategyarena.shared.indicators.IndicatorDefinition;
import com.defistrategyarena.shared.indicators.IndicatorId;
import com.defistrategyarena.shared.indicators.IndicatorParameterSpec;
import com.defistrategyarena.strategy.adapter.dsl.StrategyConditionWire;
import com.defistrategyarena.strategy.adapter.dsl.StrategyWireText;
import com.defistrategyarena.strategy.domain.CompareOperator;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Phase10ModelEdgesTest {

    private static final String EMPTY = "";

    @Test
    void wire_text_null_becomes_empty() {
        assertEquals(EMPTY, StrategyWireText.orEmpty(new StrategyWireText.TextValue(null)));
    }

    @Test
    void condition_wire_null_collections_become_empty() {
        StrategyConditionWire wire =
                new StrategyConditionWire("and", null, null, null, null, null, null);
        assertTrue(wire.children().isEmpty());
        assertTrue(wire.parameters().isEmpty());
        assertEquals(EMPTY, wire.instrument());
    }

    @Test
    void and_or_reject_null_children() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition.And(null));
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition.Or(null));
    }

    @Test
    void compare_operator_rejects_unknown_and_null_wire() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CompareOperator.fromWire(new CompareOperator.WireOperator("nope")));
        assertThrows(
                IllegalArgumentException.class,
                () -> CompareOperator.fromWire(new CompareOperator.WireOperator(null)));
    }

    @Test
    void indicator_id_and_definition_guards() {
        assertThrows(IllegalArgumentException.class, () -> new IndicatorId(" "));
        assertThrows(IllegalArgumentException.class, () -> new IndicatorId(null));
        IndicatorId id = IndicatorId.create(new IndicatorId("sma"));
        assertEquals("sma", id.value());
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndicatorDefinition(null, "SMA", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndicatorDefinition(id, " ", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndicatorDefinition(id, null, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndicatorDefinition(id, "SMA", null));
        IndicatorDefinition definition =
                IndicatorDefinition.create(new IndicatorDefinition(id, "SMA", List.of()));
        assertEquals("SMA", definition.displayName());
    }

    @Test
    void indicator_parameter_spec_guards_and_create() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IndicatorParameterSpec(
                                " ", IndicatorParameterSpec.ParameterType.INTEGER, true, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IndicatorParameterSpec(
                                null, IndicatorParameterSpec.ParameterType.INTEGER, true, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new IndicatorParameterSpec("period", null, true, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IndicatorParameterSpec(
                                "period", IndicatorParameterSpec.ParameterType.INTEGER, true, null));
        IndicatorParameterSpec spec =
                IndicatorParameterSpec.create(
                        new IndicatorParameterSpec(
                                "period", IndicatorParameterSpec.ParameterType.INTEGER, true, "14"));
        assertEquals("period", spec.name());
    }

    @Test
    void indicator_catalog_optional_and_validate_command_null_map() {
        assertTrue(IndicatorCatalog.find(new IndicatorId("nope")).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new IndicatorCatalog.ValidateParametersCommand(
                                new IndicatorId("sma"), null));
        IndicatorCatalog.validateParameters(
                new IndicatorCatalog.ValidateParametersCommand(
                        new IndicatorId("sma"), Map.of("period", "14")));
    }

    @Test
    void rule_and_leaf_condition_guards() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDefinition.Rule(
                                "r1", null, new StrategyDefinition.Hold()));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDefinition.Rule(
                                "r1",
                                new StrategyDefinition.PriceCompare(
                                        "ETH-USD", CompareOperator.GT, "1"),
                                null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StrategyDefinition.PriceCompare("ETH-USD", null, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDefinition.IndicatorCompare(
                                null, Map.of("period", "14"), CompareOperator.LT, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDefinition.IndicatorCompare(
                                new IndicatorId("sma"), null, CompareOperator.LT, "1"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new StrategyDefinition.IndicatorCompare(
                                new IndicatorId("sma"),
                                Map.of("period", "14"),
                                null,
                                "1"));
    }

    @Test
    void publish_new_version_rejects_null_description() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Strategy.PublishNewVersionData(
                                null, List.of(StrategyTestFixtures.priceGtHold("r1"))));
    }

    @Test
    void event_payloads_normalize_null_fields() {
        CreateStrategyRequested create =
                new CreateStrategyRequested("c", "o", "n", null, List.of());
        assertEquals(EMPTY, create.description());
        CreateStrategyRequested.ConditionPayload condition =
                new CreateStrategyRequested.ConditionPayload(
                        null, null, null, null, null, null, null);
        assertEquals(EMPTY, condition.type());
        assertTrue(condition.children().isEmpty());
        assertTrue(condition.parameters().isEmpty());
        CreateStrategyRequested.ActionPayload action =
                new CreateStrategyRequested.ActionPayload(null, null, null, null, null);
        assertEquals(EMPTY, action.type());
        UpdateStrategyRequested update =
                new UpdateStrategyRequested("c", "o", "s", null, List.of());
        assertEquals(EMPTY, update.description());
        GetStrategyCompleted get =
                new GetStrategyCompleted(
                        "c", "o", "s", "n", null, "PRIVATE", 1, List.of(), null, null);
        assertEquals(EMPTY, get.description());
        assertEquals(EMPTY, get.pnl());
        assertEquals(EMPTY, get.drawdown());
    }
}
