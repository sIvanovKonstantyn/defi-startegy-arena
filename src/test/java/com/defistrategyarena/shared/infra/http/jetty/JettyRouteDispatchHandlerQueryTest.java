package com.defistrategyarena.shared.infra.http.jetty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JettyRouteDispatchHandlerQueryTest {

    private static final String OWNER = "ownerId";
    private static final String VALUE = "o1";
    private static final String EMPTY = "";
    private static final String BLANK = "   ";

    @Test
    void parse_query_string_covers_null_blank_and_pairs() {
        assertTrue(JettyRouteDispatchHandler.parseQueryString(new JettyRouteDispatchHandler.RawQuery(null)).isEmpty());
        assertTrue(JettyRouteDispatchHandler.parseQueryString(new JettyRouteDispatchHandler.RawQuery(EMPTY)).isEmpty());
        assertTrue(JettyRouteDispatchHandler.parseQueryString(new JettyRouteDispatchHandler.RawQuery(BLANK)).isEmpty());
        Map<String, String> query =
                JettyRouteDispatchHandler.parseQueryString(
                        new JettyRouteDispatchHandler.RawQuery(OWNER + "=" + VALUE));
        assertEquals(VALUE, query.get(OWNER));
        Map<String, String> flag =
                JettyRouteDispatchHandler.parseQueryString(new JettyRouteDispatchHandler.RawQuery("debug"));
        assertEquals(EMPTY, flag.get("debug"));
    }
}
