package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.DeleteStrategyHttpResponse;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.UpdateStrategyHttpResponse;
import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.CreateStrategyCommand;
import com.defistrategyarena.strategy.application.OwnerStrategyName;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyAdapterEdgesTest {

    private static final int STATUS_CREATED = 201;
    private static final int STATUS_OK = 200;
    private static final int VERSION_TWO = 2;
    private static final String OWNER = "owner-a";
    private static final String NAME = "alpha";
    private static final String STRATEGY_ID = "id";

    @Test
    void http_dto_factories_and_command_factory_are_reachable() {
        CreateStrategyHttpRequest request =
                CreateStrategyHttpRequest.create(new CreateStrategyHttpRequest(NAME, List.of()));
        CreateStrategyHttpResponse response =
                CreateStrategyHttpResponse.create(new CreateStrategyHttpResponse(STATUS_CREATED, STRATEGY_ID));
        CreateStrategyCommand command =
                CreateStrategyCommand.create(
                        new CreateStrategyCommand(OWNER, new StrategyDefinition(NAME, List.of())));
        assertEquals(NAME, request.name());
        assertEquals(STATUS_CREATED, response.status());
        assertEquals(OWNER, command.ownerId());
        UpdateStrategyHttpRequest updateRequest =
                UpdateStrategyHttpRequest.create(new UpdateStrategyHttpRequest(List.of()));
        UpdateStrategyHttpResponse updateResponse =
                UpdateStrategyHttpResponse.create(
                        new UpdateStrategyHttpResponse(STATUS_OK, STRATEGY_ID, VERSION_TWO));
        DeleteStrategyHttpResponse deleteResponse =
                DeleteStrategyHttpResponse.create(
                        new DeleteStrategyHttpResponse(STATUS_OK, STRATEGY_ID));
        assertTrue(updateRequest.rules().isEmpty());
        assertEquals(VERSION_TWO, updateResponse.versionNumber());
        assertEquals(STRATEGY_ID, deleteResponse.strategyId());
    }

    @Test
    void repository_find_miss_returns_empty_and_owner_name_factory_works() {
        InMemoryStrategyRepository repository = new InMemoryStrategyRepository();
        OwnerStrategyName key = OwnerStrategyName.create(new OwnerStrategyName(OWNER, NAME));
        assertTrue(repository.findByOwnerAndName(key).isEmpty());
        assertEquals(0, repository.countByOwnerAndName(key));
    }

    @Test
    void create_strategy_rejects_null_command() {
        CreateStrategy useCase =
                new CreateStrategy(
                        new CreateStrategy.CreateStrategyDeps(
                                new InMemoryStrategyRepository(), event -> {}));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void strategy_create_rejects_null_data() {
        assertThrows(NullPointerException.class, () -> Strategy.create(null));
    }

    @Test
    void get_missing_strategy_returns_empty() {
        InMemoryStrategyRepository repository = new InMemoryStrategyRepository();
        assertTrue(repository.get(new StrategyId("00000000-0000-0000-0000-000000000001")).isEmpty());
    }

    @Test
    void null_rules_list_is_rejected_by_http_request() {
        assertThrows(
                IllegalArgumentException.class, () -> new CreateStrategyHttpRequest(NAME, null));
        assertThrows(IllegalArgumentException.class, () -> new UpdateStrategyHttpRequest(null));
    }

    @Test
    void repository_update_and_delete_edge_paths() {
        InMemoryStrategyRepository repository = new InMemoryStrategyRepository();
        Strategy strategy =
                Strategy.create(
                        new Strategy.CreateStrategyData(
                                OWNER, new StrategyDefinition(NAME, List.of())));
        assertThrows(IllegalArgumentException.class, () -> repository.update(strategy));
        repository.delete(strategy.id());
        repository.save(strategy);
        repository.delete(strategy.id());
        assertTrue(repository.get(strategy.id()).isEmpty());
    }
}
