package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.strategy.application.CreateStrategy;
import com.defistrategyarena.strategy.application.CreateStrategyCommand;
import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.defistrategyarena.strategy.application.GetStrategy;
import com.defistrategyarena.strategy.application.GetStrategyQuery;
import com.defistrategyarena.strategy.application.ListStrategies;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyPage;
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
    private static final int EMPTY_TOTAL_PAGES = 0;
    private static final int EMPTY_VERSION = 0;
    private static final long PAGE_OFFSET = 1L;
    private static final String EMPTY = "";
    private static final long EMPTY_TOTAL_ELEMENTS = 0L;

    private final CreateStrategy createStrategy;
    private final ListStrategies listStrategies;
    private final GetStrategy getStrategy;

    public StrategyRestAdapter(StrategyRestAdapterDeps deps) {
        this.createStrategy = deps.createStrategy();
        this.listStrategies = deps.listStrategies();
        this.getStrategy = deps.getStrategy();
    }

    public CreateStrategyHttpResponse create(CreateStrategyHttpRequest request) {
        try {
            StrategyId id =
                    createStrategy.execute(
                            CreateStrategyCommand.create(
                                    new CreateStrategyCommand(request.ownerId(), toDefinition(request))));
            return new CreateStrategyHttpResponse(STATUS_CREATED, id.value());
        } catch (DuplicateStrategyException exception) {
            return new CreateStrategyHttpResponse(STATUS_CONFLICT, EMPTY);
        } catch (IllegalArgumentException exception) {
            return new CreateStrategyHttpResponse(STATUS_BAD_REQUEST, EMPTY);
        }
    }

    public StrategyListHttpResponse list(ListStrategiesQuery query) {
        StrategyPage page = listStrategies.execute(query);
        return toListResponse(new ListResponseInput(query, page));
    }

    public StrategyDetailHttpResponse get(GetStrategyQuery query) {
        Optional<Strategy> found = getStrategy.execute(query);
        if (found.isEmpty()) {
            return notFoundDetail();
        }
        return toDetail(found.get());
    }

    private static StrategyDefinition toDefinition(CreateStrategyHttpRequest request) {
        List<StrategyDefinition.Rule> rules = new ArrayList<>();
        for (CreateStrategyHttpRequest.RuleBody body : request.rules()) {
            rules.add(CreateStrategyRuleMapper.toRule(body));
        }
        return StrategyDefinition.create(new StrategyDefinition(request.name(), rules));
    }

    private static StrategyListHttpResponse toListResponse(ListResponseInput input) {
        List<StrategySummaryHttpResponse> items = new ArrayList<>();
        for (Strategy strategy : input.page().items()) {
            items.add(toSummary(strategy));
        }
        ListStrategiesQuery query = input.query();
        return new StrategyListHttpResponse(
                STATUS_OK,
                query.page(),
                query.size(),
                input.page().totalElements(),
                totalPages(new TotalPagesInput(input.page().totalElements(), query.size())),
                query.sort(),
                query.order(),
                items);
    }

    private static StrategySummaryHttpResponse toSummary(Strategy strategy) {
        StrategyViewFields fields = viewFields(strategy);
        return new StrategySummaryHttpResponse(
                fields.strategyId(), fields.name(), fields.privacy(), fields.versionNumber());
    }

    private static StrategyDetailHttpResponse toDetail(Strategy strategy) {
        StrategyViewFields fields = viewFields(strategy);
        return new StrategyDetailHttpResponse(
                STATUS_OK,
                fields.strategyId(),
                fields.name(),
                fields.privacy(),
                fields.versionNumber(),
                StrategyRuleHttpMapper.toBodies(strategy.current().definition().rules()));
    }

    private static StrategyViewFields viewFields(Strategy strategy) {
        return new StrategyViewFields(
                strategy.id().value(),
                strategy.current().definition().name(),
                strategy.privacy().name(),
                strategy.current().number());
    }

    private static int totalPages(TotalPagesInput input) {
        if (input.totalElements() == EMPTY_TOTAL_ELEMENTS) {
            return EMPTY_TOTAL_PAGES;
        }
        long pages = (input.totalElements() + input.size() - PAGE_OFFSET) / input.size();
        return (int) pages;
    }

    private static StrategyDetailHttpResponse notFoundDetail() {
        return new StrategyDetailHttpResponse(
                STATUS_NOT_FOUND, EMPTY, EMPTY, EMPTY, EMPTY_VERSION, List.of());
    }

    private record ListResponseInput(ListStrategiesQuery query, StrategyPage page) {}

    private record TotalPagesInput(long totalElements, int size) {}

    private record StrategyViewFields(
            String strategyId, String name, String privacy, int versionNumber) {}

    public record StrategyRestAdapterDeps(
            CreateStrategy createStrategy, ListStrategies listStrategies, GetStrategy getStrategy) {}
}
