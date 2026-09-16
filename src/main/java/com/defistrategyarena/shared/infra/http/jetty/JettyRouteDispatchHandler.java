package com.defistrategyarena.shared.infra.http.jetty;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteLookup;
import com.defistrategyarena.shared.infra.http.HttpRouteMatch;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public final class JettyRouteDispatchHandler extends Handler.Abstract {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String NOT_FOUND_BODY = "not found";
    private static final String PLAIN_TEXT = "text/plain";
    private static final String QUERY_PAIR_SEPARATOR = "&";
    private static final String QUERY_KV_SEPARATOR = "=";
    private static final String EMPTY_VALUE = "";
    private static final int INDEX_NOT_FOUND = -1;
    private static final int VALUE_OFFSET = 1;
    private static final int NAME_START = 0;

    private final HttpRouteRegistry routes;

    public JettyRouteDispatchHandler(HttpRouteRegistry routes) {
        this.routes = routes;
    }

    @Override
    @SuppressWarnings("PMD.MethodsTakeAtMostOneDtoParameter")
    public boolean handle(Request request, Response response, Callback callback) throws IOException {
        String method = request.getMethod();
        String path = Request.getPathInContext(request);
        HttpRouteLookup lookup = HttpRouteLookup.create(new HttpRouteLookup(method, path));
        Optional<HttpRouteMatch> match = routes.find(lookup);
        if (match.isEmpty()) {
            writeResponse(
                    JettyResponseWriteData.create(
                            new JettyResponseWriteData(
                                    response,
                                    callback,
                                    new HttpResponse(HttpStatus.NOT_FOUND_404, PLAIN_TEXT, NOT_FOUND_BODY))));
            return true;
        }

        HttpRouteMatch route = match.get();
        HttpRequest httpRequest =
                new HttpRequest(
                        method,
                        path,
                        readBody(request),
                        parseQuery(new RawQuery(request.getHttpURI().getQuery())),
                        route.pathVariables(),
                        readHeaders(request));
        HttpHandler handler = route.handler();
        HttpResponse httpResponse = handler.handle(httpRequest);
        writeResponse(
                JettyResponseWriteData.create(
                        new JettyResponseWriteData(response, callback, httpResponse)));
        return true;
    }

    private static String readBody(Request request) throws IOException {
        try (InputStream input = Request.asInputStream(request)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> readHeaders(Request request) {
        String authorization = request.getHeaders().get(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank()) {
            return Map.of();
        }
        return Map.of(AUTHORIZATION_HEADER, authorization);
    }

    private static Map<String, String> parseQuery(RawQuery rawQuery) {
        return parseQueryString(rawQuery);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    static Map<String, String> parseQueryString(RawQuery rawQuery) {
        String raw = rawQuery.value();
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, String> query = new LinkedHashMap<>();
        for (String pair : raw.split(QUERY_PAIR_SEPARATOR)) {
            putPair(new QueryPair(pair, query));
        }
        return Map.copyOf(query);
    }

    private static void putPair(QueryPair pair) {
        int separator = pair.raw().indexOf(QUERY_KV_SEPARATOR);
        if (separator == INDEX_NOT_FOUND) {
            pair.query().put(decode(new EncodedText(pair.raw())), EMPTY_VALUE);
            return;
        }
        String name = decode(new EncodedText(pair.raw().substring(NAME_START, separator)));
        String value = decode(new EncodedText(pair.raw().substring(separator + VALUE_OFFSET)));
        pair.query().put(name, value);
    }

    private static String decode(EncodedText text) {
        return URLDecoder.decode(text.value(), StandardCharsets.UTF_8);
    }

    private static void writeResponse(JettyResponseWriteData writeData) {
        Response response = writeData.response();
        Callback callback = writeData.callback();
        HttpResponse httpResponse = writeData.httpResponse();
        response.setStatus(httpResponse.status());
        response.getHeaders().put(CONTENT_TYPE_HEADER, httpResponse.contentType());
        byte[] payload = httpResponse.body().getBytes(StandardCharsets.UTF_8);
        response.write(true, ByteBuffer.wrap(payload), callback);
    }

    private record EncodedText(String value) {}

    private record QueryPair(String raw, Map<String, String> query) {}

    record RawQuery(String value) {}
}
