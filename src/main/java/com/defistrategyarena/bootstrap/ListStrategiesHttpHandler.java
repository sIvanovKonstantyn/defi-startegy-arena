package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.adapter.web.StrategyListHttpResponse;
import com.defistrategyarena.strategy.adapter.web.StrategyRestAdapter;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import java.util.List;
import java.util.Map;

public final class ListStrategiesHttpHandler extends BaseHandler<StrategyListHttpResponse> {

    private static final String OWNER_ID = "ownerId";
    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
    private static final String ORDER = "order";
    private static final String EMPTY = "";
    private static final String INVALID_INTEGER = "invalid integer query parameter";
    private static final int EMPTY_TOTAL_PAGES = 0;
    private static final long EMPTY_TOTAL_ELEMENTS = 0L;

    private final StrategyRestAdapter strategyHttp;

    public ListStrategiesHttpHandler(ListStrategiesHttpHandlerDeps deps) {
        this.strategyHttp = deps.strategyHttp();
    }

    @Override
    protected StrategyListHttpResponse execute(HttpRequest request) {
        return strategyHttp.list(toQuery(new QueryBag(request.query())));
    }

    @Override
    protected StrategyListHttpResponse badRequestBody() {
        return new StrategyListHttpResponse(
                STATUS_BAD_REQUEST,
                ListStrategiesQuery.DEFAULT_PAGE,
                ListStrategiesQuery.DEFAULT_SIZE,
                EMPTY_TOTAL_ELEMENTS,
                EMPTY_TOTAL_PAGES,
                ListStrategiesQuery.SORT_NAME,
                ListStrategiesQuery.ORDER_ASC,
                List.of());
    }

    private static ListStrategiesQuery toQuery(QueryBag query) {
        return new ListStrategiesQuery(
                query.text(new QueryKey(OWNER_ID)),
                query.integerOrDefault(new IntParam(PAGE, ListStrategiesQuery.DEFAULT_PAGE)),
                query.integerOrDefault(new IntParam(SIZE, ListStrategiesQuery.DEFAULT_SIZE)),
                query.textOrDefault(new TextParam(SORT, ListStrategiesQuery.SORT_NAME)),
                query.textOrDefault(new TextParam(ORDER, ListStrategiesQuery.ORDER_ASC)));
    }

    private record QueryBag(Map<String, String> values) {
        private String text(QueryKey key) {
            String value = values.get(key.name());
            if (value == null) {
                return EMPTY;
            }
            return value;
        }

        private String textOrDefault(TextParam param) {
            String value = values.get(param.key());
            if (value == null || value.isBlank()) {
                return param.defaultValue();
            }
            return value;
        }

        private int integerOrDefault(IntParam param) {
            String value = values.get(param.key());
            if (value == null || value.isBlank()) {
                return param.defaultValue();
            }
            return parseInteger(new RawInteger(value));
        }

        private static int parseInteger(RawInteger raw) {
            try {
                return Integer.parseInt(raw.value());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(INVALID_INTEGER, exception);
            }
        }
    }

    private record QueryKey(String name) {}

    private record TextParam(String key, String defaultValue) {}

    private record IntParam(String key, int defaultValue) {}

    private record RawInteger(String value) {}

    public record ListStrategiesHttpHandlerDeps(StrategyRestAdapter strategyHttp) {}
}
