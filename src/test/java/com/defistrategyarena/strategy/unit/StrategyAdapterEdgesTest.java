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
import com.defistrategyarena.strategy.domain.StrategyId;
import com.defistrategyarena.strategy.testsupport.StrategyTestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrategyAdapterEdgesTest {

    private static final int STATUS_CREATED = 201;
    private static final int STATUS_OK = 200;
    private static final int VERSION_TWO = 2;
    private static final int NO_MATCHES = 0;
    private static final String OWNER = "owner-a";
    private static final String NAME = "alpha";
    private static final String STRATEGY_ID = "id";
    private static final String RULE_ID = "r1";
    private static final String DESCRIPTION = StrategyTestFixtures.DESCRIPTION;
    private static final String EMPTY = StrategyTestFixtures.EMPTY;
    private static final String UNKNOWN_STRATEGY_ID = "00000000-0000-0000-0000-000000000001";

    @Test
    void http_dto_factories_and_command_factory_are_reachable() {
        CreateStrategyHttpRequest request =
                CreateStrategyHttpRequest.create(
                        new CreateStrategyHttpRequest(NAME, DESCRIPTION, List.of()));
        CreateStrategyHttpResponse response =
                CreateStrategyHttpResponse.create(new CreateStrategyHttpResponse(STATUS_CREATED, STRATEGY_ID));
        CreateStrategyCommand command =
                CreateStrategyCommand.create(
                        new CreateStrategyCommand(
                                OWNER, StrategyTestFixtures.emptyDefinition(NAME)));
        assertEquals(NAME, request.name());
        assertEquals(DESCRIPTION, request.description());
        assertEquals(STATUS_CREATED, response.status());
        assertEquals(OWNER, command.ownerId());
        UpdateStrategyHttpRequest updateRequest =
                UpdateStrategyHttpRequest.create(
                        new UpdateStrategyHttpRequest(DESCRIPTION, List.of()));
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
        assertEquals(NO_MATCHES, repository.countByOwnerAndName(key));
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
        assertTrue(repository.get(new StrategyId(UNKNOWN_STRATEGY_ID)).isEmpty());
    }

    @Test
    void null_rules_list_is_rejected_by_http_request() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CreateStrategyHttpRequest(NAME, DESCRIPTION, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new UpdateStrategyHttpRequest(DESCRIPTION, null));
    }

    @Test
    void null_description_defaults_to_empty_on_http_requests() {
        assertEquals(EMPTY, new CreateStrategyHttpRequest(NAME, null, List.of()).description());
        assertEquals(EMPTY, new UpdateStrategyHttpRequest(null, List.of()).description());
    }

    @Test
    void condition_and_action_bodies_default_null_fields_to_empty() {
        CreateStrategyHttpRequest.ConditionBody condition =
                new CreateStrategyHttpRequest.ConditionBody(null, null, null, null, null, null, null);
        CreateStrategyHttpRequest.ActionBody action =
                new CreateStrategyHttpRequest.ActionBody(null, null, null, null, null);
        assertEquals(EMPTY, condition.type());
        assertEquals(EMPTY, condition.instrument());
        assertEquals(EMPTY, condition.indicator());
        assertEquals(EMPTY, condition.operator());
        assertEquals(EMPTY, condition.threshold());
        assertTrue(condition.children().isEmpty());
        assertTrue(condition.parameters().isEmpty());
        assertEquals(EMPTY, action.type());
        assertEquals(EMPTY, action.instrument());
        assertEquals(EMPTY, action.instrumentPair());
        assertEquals(EMPTY, action.allocationPercent());
        assertEquals(EMPTY, action.yearlyFeePercent());
    }

    @Test
    void rule_body_round_trips_through_event_payload() {
        CreateStrategyHttpRequest.RuleBody body = StrategyTestFixtures.andOpenLpBody(RULE_ID);
        CreateStrategyHttpRequest.RuleBody restored =
                CreateStrategyHttpRequest.RuleBody.fromEvent(body.toEvent());
        assertEquals(body, restored);
    }

    @Test
    void repository_update_and_delete_edge_paths() {
        InMemoryStrategyRepository repository = new InMemoryStrategyRepository();
        Strategy strategy =
                Strategy.create(
                        new Strategy.CreateStrategyData(
                                OWNER, StrategyTestFixtures.emptyDefinition(NAME)));
        assertThrows(IllegalArgumentException.class, () -> repository.update(strategy));
        repository.delete(strategy.id());
        repository.save(strategy);
        repository.delete(strategy.id());
        assertTrue(repository.get(strategy.id()).isEmpty());
    }
}
