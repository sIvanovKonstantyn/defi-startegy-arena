package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.application.CreateStrategyCommand;
import com.defistrategyarena.strategy.application.DeleteStrategyCommand;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.application.StrategyUseCases;
import com.defistrategyarena.strategy.application.UpdateStrategyCommand;
import com.defistrategyarena.strategy.application.UpdateStrategyResult;
import com.defistrategyarena.strategy.domain.Strategy;
import com.defistrategyarena.strategy.domain.StrategyDefinition;
import com.defistrategyarena.strategy.domain.StrategyId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StrategyRestAdapter {

    private static final int STATUS_OK = 200;
    private static final int STATUS_CREATED = 201;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_CONFLICT = 409;
    private static final int EMPTY_VERSION = 0;
    private static final String EMPTY = "";

    private final StrategyUseCases useCases;

    public StrategyRestAdapter(StrategyRestAdapterDeps deps) {
        this.useCases = deps.useCases();
    }

    public CreateStrategyHttpResponse create(CreateStrategyHttpInput input) {
        try {
            StrategyId id =
                    useCases.createStrategy()
                            .execute(
                                    CreateStrategyCommand.create(
                                            new CreateStrategyCommand(
                                                    input.ownerId(), toDefinition(input.request()))));
            return new CreateStrategyHttpResponse(STATUS_CREATED, id.value());
        } catch (DuplicateStrategyException exception) {
            return new CreateStrategyHttpResponse(STATUS_CONFLICT, EMPTY);
        } catch (IllegalArgumentException exception) {
            return new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY);
        }
    }

    public StrategyListHttpResponse list(ListStrategiesQuery query) {
        StrategyPage page = useCases.listStrategies().execute(query);
        return StrategyHttpViews.toListResponse(new StrategyHttpViews.ListResponseInput(query, page));
    }

    public StrategyDetailHttpResponse get(GetStrategyQuery query) {
        Optional<Strategy> found = useCases.getStrategy().execute(query);
        if (found.isEmpty()) {
            return StrategyHttpViews.notFoundDetail();
        }
        return StrategyHttpViews.toDetail(found.get());
    }

    public UpdateStrategyHttpResponse update(UpdateStrategyHttpInput input) {
        try {
            List<StrategyDefinition.Rule> rules = toRules(input.request().rules());
            Optional<UpdateStrategyResult> result =
                    useCases.updateStrategy()
                            .execute(
                                    new UpdateStrategyCommand(
                                            input.ownerId(),
                                            input.strategyId(),
                                            input.request().description(),
                                            rules));
            if (result.isEmpty()) {
                return new UpdateStrategyHttpResponse(STATUS_NOT_FOUND, EMPTY, EMPTY_VERSION);
            }
            UpdateStrategyResult updated = result.get();
            return new UpdateStrategyHttpResponse(
                    STATUS_OK, updated.strategyId().value(), updated.versionNumber());
        } catch (IllegalArgumentException exception) {
            return new UpdateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY, EMPTY_VERSION);
        }
    }

    public DeleteStrategyHttpResponse delete(DeleteStrategyHttpInput input) {
        try {
            Optional<StrategyId> deleted =
                    useCases.deleteStrategy()
                            .execute(
                                    new DeleteStrategyCommand(input.ownerId(), input.strategyId()));
            if (deleted.isEmpty()) {
                return new DeleteStrategyHttpResponse(STATUS_NOT_FOUND, EMPTY);
            }
            return new DeleteStrategyHttpResponse(STATUS_OK, deleted.get().value());
        } catch (IllegalArgumentException exception) {
            return new DeleteStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY);
        }
    }

    private static StrategyDefinition toDefinition(CreateStrategyHttpRequest request) {
        return StrategyDefinition.create(
                new StrategyDefinition(request.name(), request.description(), toRules(request.rules())));
    }

    private static List<StrategyDefinition.Rule> toRules(List<CreateStrategyHttpRequest.RuleBody> bodies) {
        List<StrategyDefinition.Rule> rules = new ArrayList<>();
        for (CreateStrategyHttpRequest.RuleBody body : bodies) {
            rules.add(CreateStrategyRuleMapper.toRule(body));
        }
        return List.copyOf(rules);
    }

    public record CreateStrategyHttpInput(String ownerId, CreateStrategyHttpRequest request) {}

    public record UpdateStrategyHttpInput(
            String ownerId, StrategyId strategyId, UpdateStrategyHttpRequest request) {}

    public record DeleteStrategyHttpInput(String ownerId, StrategyId strategyId) {}

    public record StrategyRestAdapterDeps(StrategyUseCases useCases) {}
}
