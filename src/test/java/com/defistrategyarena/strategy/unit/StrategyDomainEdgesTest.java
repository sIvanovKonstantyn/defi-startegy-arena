package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.strategy.domain.Privacy;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.domain.StrategyVersion;
import com.defistrategyarena.strategy.application.DeleteStrategyCommand;
import com.defistrategyarena.strategy.application.UpdateStrategyCommand;
import com.defistrategyarena.strategy.application.UpdateStrategyResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyDomainEdgesTest {

    private static final int VERSION_TWO = 2;

    @Test
    void strategy_id_rejects_blank() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyId(" "));
    }

    @Test
    void strategy_id_rejects_null() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyId(null));
    }

    @Test
    void strategy_version_create_copies_draft() {
        StrategyDefinition definition = new StrategyDefinition("n", List.of());
        StrategyVersion version =
                StrategyVersion.create(new StrategyVersion(VERSION_TWO, definition));
        assertEquals(VERSION_TWO, version.number());
        assertEquals(definition, version.definition());
    }

    @Test
    void strategy_version_rejects_null_definition() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyVersion(VERSION_TWO, null));
    }

    @Test
    void privacy_catalog_includes_shared() {
        assertEquals(Privacy.SHARED, Privacy.valueOf("SHARED"));
    }

    @Test
    void strategy_definition_rejects_null_rules() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition("n", null));
    }

    @Test
    void strategy_definition_rejects_null_name() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition(null, List.of()));
    }

    @Test
    void strategy_create_rejects_null_owner() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Strategy.create(new Strategy.CreateStrategyData(null, new StrategyDefinition("n", List.of()))));
    }

    @Test
    void buy_rejects_blank_allocation() {
        assertThrows(
                IllegalArgumentException.class, () -> new StrategyDefinition.Buy("ETH-USD", " "));
    }

    @Test
    void buy_rejects_null_instrument() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition.Buy(null, "10"));
    }

    @Test
    void sell_rejects_blank_instrument() {
        assertThrows(IllegalArgumentException.class, () -> new StrategyDefinition.Sell(" ", "10"));
    }

    @Test
    void strategy_publish_new_version_rejects_empty_rules() {
        Strategy strategy =
                Strategy.create(
                        new Strategy.CreateStrategyData(
                                "owner", new StrategyDefinition("n", List.of())));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Strategy.PublishNewVersionData(List.of()));
        assertThrows(
                IllegalArgumentException.class, () -> new Strategy.PublishNewVersionData(null));
        assertThrows(NullPointerException.class, () -> strategy.publishNewVersion(null));
    }

    @Test
    void strategy_version_next_increments() {
        StrategyDefinition definition = new StrategyDefinition("n", List.of());
        StrategyVersion next = StrategyVersion.initial(definition).next(definition);
        assertEquals(VERSION_TWO, next.number());
    }

    @Test
    void update_and_delete_command_factories_and_guards() {
        StrategyId id = new StrategyId("00000000-0000-0000-0000-000000000001");
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateStrategyCommand(null, id, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateStrategyCommand("o", null, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateStrategyCommand("o", id, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteStrategyCommand(null, id));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeleteStrategyCommand("o", null));
        UpdateStrategyResult result =
                UpdateStrategyResult.create(new UpdateStrategyResult(id, VERSION_TWO));
        assertEquals(VERSION_TWO, result.versionNumber());
        DeleteStrategyCommand delete = DeleteStrategyCommand.create(new DeleteStrategyCommand("o", id));
        assertEquals("o", delete.ownerId());
    }
}
