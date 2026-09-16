package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.events.strategy.ListStrategiesRequested;
import com.defistrategyarena.shared.http.handlers.BaseHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.strategy.application.ListStrategiesQuery;
import java.util.Map;

public final class ListStrategiesHttpHandler extends BaseHandler<AcceptedHttpResponse> {

    private static final int STATUS_ACCEPTED = 202;
    private static final int STATUS_UNAUTHORIZED = 401;
    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final String SORT = "sort";
    private static final String ORDER = "order";
    private static final String INVALID_INTEGER = "invalid integer query parameter";

    private final AuthenticatedStrategyPublisher publisher;

    public ListStrategiesHttpHandler(ListStrategiesHttpHandlerDeps deps) {
        this.publisher = deps.publisher();
    }

    @Override
    protected AcceptedHttpResponse execute(HttpRequest request) {
        QueryBag query = new QueryBag(request.query());
        int page = query.integerOrDefault(new IntParam(PAGE, ListStrategiesQuery.DEFAULT_PAGE));
        int size = query.integerOrDefault(new IntParam(SIZE, ListStrategiesQuery.DEFAULT_SIZE));
        String sort = query.textOrDefault(new TextParam(SORT, ListStrategiesQuery.SORT_NAME));
        String order = query.textOrDefault(new TextParam(ORDER, ListStrategiesQuery.ORDER_ASC));
        return publisher.publish(
                new AuthenticatedStrategyPublisher.AuthorizedPublish(
                        request,
                        context ->
                                new ListStrategiesRequested(
                                        context.correlationId(),
                                        context.ownerId(),
                                        page,
                                        size,
                                        sort,
                                        order),
                        STATUS_ACCEPTED,
                        STATUS_UNAUTHORIZED));
    }

    @Override
    protected AcceptedHttpResponse badRequestBody() {
        return AcceptedResponses.badRequest();
    }

    private record QueryBag(Map<String, String> values) {
        private String textOrDefault(TextParam param) {
            String value = values.get(param.key());
            if (blank(new RawText(value))) {
                return param.defaultValue();
            }
            return value;
        }

        private int integerOrDefault(IntParam param) {
            String value = values.get(param.key());
            if (blank(new RawText(value))) {
                return param.defaultValue();
            }
            return parseInteger(new RawText(value));
        }

        private static boolean blank(RawText text) {
            return text.value() == null || text.value().isBlank();
        }

        private static int parseInteger(RawText text) {
            try {
                return Integer.parseInt(text.value());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(INVALID_INTEGER, exception);
            }
        }
    }

    private record RawText(String value) {}

    private record TextParam(String key, String defaultValue) {}

    private record IntParam(String key, int defaultValue) {}

    public record ListStrategiesHttpHandlerDeps(AuthenticatedStrategyPublisher publisher) {}
}
