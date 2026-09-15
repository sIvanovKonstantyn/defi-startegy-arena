package com.defistrategyarena.strategy.adapter.web;

import com.defistrategyarena.shared.http.JsonHttpResult;
import java.util.List;

public record StrategyListHttpResponse(
        int status,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort,
        String order,
        List<StrategySummaryHttpResponse> items)
        implements JsonHttpResult {

    private static final String ITEMS_REQUIRED = "items must not be null";

    public StrategyListHttpResponse {
        if (items == null) {
            throw new IllegalArgumentException(ITEMS_REQUIRED);
        }
        items = List.copyOf(items);
    }

    public static StrategyListHttpResponse create(StrategyListHttpResponse draft) {
        return new StrategyListHttpResponse(
                draft.status(),
                draft.page(),
                draft.size(),
                draft.totalElements(),
                draft.totalPages(),
                draft.sort(),
                draft.order(),
                draft.items());
    }
}
