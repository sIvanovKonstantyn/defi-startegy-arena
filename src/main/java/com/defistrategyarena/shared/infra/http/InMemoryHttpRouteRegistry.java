package com.defistrategyarena.shared.infra.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
public final class InMemoryHttpRouteRegistry implements HttpRouteRegistry {

    private static final char TEMPLATE_OPEN = '{';
    private static final char TEMPLATE_CLOSE = '}';
    private static final String PATH_SEPARATOR = "/";
    private static final int TEMPLATE_NAME_START = 1;
    private static final int FIRST_CHAR_INDEX = 0;
    private static final int FIRST_SEGMENT_INDEX = 0;
    private static final int RETAIN_EMPTY_SEGMENTS = -1;
    private static final int INDEX_NOT_FOUND = -1;

    private final Map<RouteKey, HttpHandler> exactRoutes = new HashMap<>();
    private final List<TemplateRoute> templateRoutes = new ArrayList<>();

    @Override
    public void register(HttpRouteRegistration registration) {
        String path = registration.path();
        if (path.indexOf(TEMPLATE_OPEN) != INDEX_NOT_FOUND) {
            RouteKey methodKey = RouteKey.create(new RouteKeyData(registration.method(), path));
            templateRoutes.add(new TemplateRoute(methodKey.method(), path, registration.handler()));
            return;
        }
        RouteKey key = RouteKey.create(new RouteKeyData(registration.method(), path));
        exactRoutes.put(key, registration.handler());
    }

    @Override
    public Optional<HttpRouteMatch> find(HttpRouteLookup lookup) {
        RouteKey key = RouteKey.create(new RouteKeyData(lookup.method(), lookup.path()));
        HttpHandler exact = exactRoutes.get(key);
        if (exact != null) {
            return Optional.of(new HttpRouteMatch(exact, Map.of()));
        }
        return findTemplate(new TemplateSearch(lookup, key.method()));
    }

    private Optional<HttpRouteMatch> findTemplate(TemplateSearch search) {
        for (TemplateRoute template : templateRoutes) {
            Optional<HttpRouteMatch> matched = matchTemplateRoute(new TemplateAttempt(template, search));
            if (matched.isPresent()) {
                return matched;
            }
        }
        return Optional.empty();
    }

    private static Optional<HttpRouteMatch> matchTemplateRoute(TemplateAttempt attempt) {
        if (!attempt.template().method().equals(attempt.search().method())) {
            return Optional.empty();
        }
        Optional<Map<String, String>> variables =
                matchTemplate(new PathPair(attempt.template().pathPattern(), attempt.search().lookup().path()));
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new HttpRouteMatch(attempt.template().handler(), variables.get()));
    }

    private static Optional<Map<String, String>> matchTemplate(PathPair paths) {
        String[] patternParts = paths.pattern().split(PATH_SEPARATOR, RETAIN_EMPTY_SEGMENTS);
        String[] pathParts = paths.path().split(PATH_SEPARATOR, RETAIN_EMPTY_SEGMENTS);
        if (patternParts.length != pathParts.length) {
            return Optional.empty();
        }
        Map<String, String> variables = new HashMap<>();
        int index = FIRST_SEGMENT_INDEX;
        while (index < patternParts.length) {
            Optional<String> rejected =
                    bindSegment(new SegmentPair(patternParts[index], pathParts[index], variables));
            if (rejected.isPresent()) {
                return Optional.empty();
            }
            index++;
        }
        return Optional.of(Map.copyOf(variables));
    }

    private static Optional<String> bindSegment(SegmentPair segment) {
        if (isTemplate(new TemplatePart(segment.patternPart()))) {
            String name =
                    segment.patternPart()
                            .substring(
                                    TEMPLATE_NAME_START,
                                    segment.patternPart().length() - TEMPLATE_NAME_START);
            segment.variables().put(name, segment.pathPart());
            return Optional.empty();
        }
        if (segment.patternPart().equals(segment.pathPart())) {
            return Optional.empty();
        }
        return Optional.of(segment.pathPart());
    }

    private static boolean isTemplate(TemplatePart part) {
        String value = part.value();
        return value.length() > TEMPLATE_NAME_START
                && value.charAt(FIRST_CHAR_INDEX) == TEMPLATE_OPEN
                && value.charAt(value.length() - TEMPLATE_NAME_START) == TEMPLATE_CLOSE;
    }

    private record RouteKey(String method, String path) {
        private static RouteKey create(RouteKeyData data) {
            return new RouteKey(data.method().toUpperCase(Locale.ROOT), data.path());
        }
    }

    private record RouteKeyData(String method, String path) {}

    private record TemplateRoute(String method, String pathPattern, HttpHandler handler) {}

    private record TemplateSearch(HttpRouteLookup lookup, String method) {}

    private record TemplateAttempt(TemplateRoute template, TemplateSearch search) {}

    private record PathPair(String pattern, String path) {}

    private record SegmentPair(String patternPart, String pathPart, Map<String, String> variables) {}

    private record TemplatePart(String value) {}
}
