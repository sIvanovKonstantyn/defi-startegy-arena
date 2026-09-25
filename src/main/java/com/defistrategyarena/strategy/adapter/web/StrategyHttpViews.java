package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.events.strategy.GetStrategyCompleted;
import com.defistrategyarena.shared.events.strategy.ListStrategiesCompleted;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import com.defistrategyarena.strategy.application.StrategyPage;
import com.defistrategyarena.strategy.domain.Strategy;
import java.util.ArrayList;
import java.util.List;

public enum StrategyHttpViews {
    ;

    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int EMPTY_TOTAL_PAGES = 0;
    private static final int EMPTY_VERSION = 0;
    private static final long PAGE_OFFSET = 1L;
    private static final String EMPTY = "";
    private static final long EMPTY_TOTAL_ELEMENTS = 0L;

    static StrategyListHttpResponse toListResponse(ListResponseInput input) {
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

    static StrategyDetailHttpResponse toDetail(Strategy strategy) {
        StrategyViewFields fields = viewFields(strategy);
        return new StrategyDetailHttpResponse(
                STATUS_OK,
                fields.strategyId(),
                fields.name(),
                fields.description(),
                fields.privacy(),
                fields.versionNumber(),
                StrategyRuleHttpMapper.toBodies(strategy.current().definition().rules()),
                GetStrategyCompleted.PENDING_METRIC,
                GetStrategyCompleted.PENDING_METRIC);
    }

    static StrategyDetailHttpResponse notFoundDetail() {
        return new StrategyDetailHttpResponse(
                STATUS_NOT_FOUND,
                EMPTY,
                EMPTY,
                EMPTY,
                EMPTY,
                EMPTY_VERSION,
                List.of(),
                GetStrategyCompleted.PENDING_METRIC,
                GetStrategyCompleted.PENDING_METRIC);
    }

    private static StrategySummaryHttpResponse toSummary(Strategy strategy) {
        StrategyViewFields fields = viewFields(strategy);
        return new StrategySummaryHttpResponse(
                fields.strategyId(),
                fields.name(),
                fields.description(),
                fields.privacy(),
                fields.versionNumber());
    }

    private static StrategyViewFields viewFields(Strategy strategy) {
        return new StrategyViewFields(
                strategy.id().value(),
                strategy.current().definition().name(),
                strategy.current().definition().description(),
                strategy.privacy().name(),
                strategy.current().number());
    }

    public static StrategyProjection projection(Strategy strategy) {
        StrategyViewFields fields = viewFields(strategy);
        return new StrategyProjection(
                fields.strategyId(),
                fields.name(),
                fields.description(),
                fields.privacy(),
                fields.versionNumber());
    }

    public static ListStrategiesCompleted.StrategySummaryPayload toEventSummary(Strategy strategy) {
        StrategyProjection fields = projection(strategy);
        return new ListStrategiesCompleted.StrategySummaryPayload(
                fields.strategyId(),
                fields.name(),
                fields.description(),
                fields.privacy(),
                fields.versionNumber());
    }

    public record StrategyProjection(
            String strategyId,
            String name,
            String description,
            String privacy,
            int versionNumber) {}

    private static int totalPages(TotalPagesInput input) {
        if (input.totalElements() == EMPTY_TOTAL_ELEMENTS) {
            return EMPTY_TOTAL_PAGES;
        }
        long pages = (input.totalElements() + input.size() - PAGE_OFFSET) / input.size();
        return (int) pages;
    }

    record ListResponseInput(ListStrategiesQuery query, StrategyPage page) {}

    private record TotalPagesInput(long totalElements, int size) {}

    private record StrategyViewFields(
            String strategyId,
            String name,
            String description,
            String privacy,
            int versionNumber) {}
}
