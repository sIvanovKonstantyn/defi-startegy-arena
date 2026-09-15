package com.defistrategyarena.strategy.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.strategy.adapter.persistence.InMemoryStrategyRepository;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpRequest;
import com.defistrategyarena.strategy.adapter.web.CreateStrategyHttpResponse;
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
    private static final String OWNER = "owner-a";
    private static final String NAME = "alpha";

    @Test
    void http_dto_factories_and_command_factory_are_reachable() {
        CreateStrategyHttpRequest request =
                CreateStrategyHttpRequest.create(new CreateStrategyHttpRequest(OWNER, NAME, List.of()));
        CreateStrategyHttpResponse response =
                CreateStrategyHttpResponse.create(new CreateStrategyHttpResponse(STATUS_CREATED, "id"));
        CreateStrategyCommand command =
                CreateStrategyCommand.create(
                        new CreateStrategyCommand(OWNER, new StrategyDefinition(NAME, List.of())));
        assertEquals(OWNER, request.ownerId());
        assertEquals(STATUS_CREATED, response.status());
        assertEquals(OWNER, command.ownerId());
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
                IllegalArgumentException.class, () -> new CreateStrategyHttpRequest(OWNER, NAME, null));
    }
}
