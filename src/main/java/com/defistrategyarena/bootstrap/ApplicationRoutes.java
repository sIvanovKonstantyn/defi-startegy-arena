package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRequest;
import com.defistrategyarena.shared.infra.http.HttpResponse;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;
import com.defistrategyarena.shared.infra.http.InMemoryHttpRouteRegistry;
import java.util.Objects;

public enum ApplicationRoutes {
    ;

    private static final int STATUS_OK = 200;
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";
    private static final String PATH_HEALTH = "/health";
    private static final String PATH_STRATEGIES = "/strategies";
    private static final String PATH_STRATEGY_DETAIL = "/strategies/{strategyId}";
    private static final String PLAIN_TEXT = "text/plain";
    private static final String HEALTH_BODY = "ok";

    public static HttpRouteRegistry createDefaultRoutes() {
        return createDefaultRoutes(ApplicationComposition.createDefault());
    }

    public static HttpRouteRegistry createDefaultRoutes(ApplicationComposition composition) {
        Objects.requireNonNull(composition);
        HttpRouteRegistry routes = new InMemoryHttpRouteRegistry();
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(METHOD_GET, PATH_HEALTH, healthHandler())));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_POST, PATH_STRATEGIES, createStrategyHandler(composition))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_GET, PATH_STRATEGIES, listStrategiesHandler(composition))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_GET, PATH_STRATEGY_DETAIL, getStrategyHandler(composition))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_PUT, PATH_STRATEGY_DETAIL, updateStrategyHandler(composition))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_DELETE, PATH_STRATEGY_DETAIL, deleteStrategyHandler(composition))));
        IdentityRoutes.register(new IdentityRoutes.RouteRegistrationTarget(routes, composition));
        return routes;
    }

    private static HttpHandler healthHandler() {
        return ApplicationRoutes::healthResponse;
    }

    private static HttpResponse healthResponse(HttpRequest request) {
        Objects.requireNonNull(request);
        return new HttpResponse(STATUS_OK, PLAIN_TEXT, HEALTH_BODY);
    }

    private static HttpHandler createStrategyHandler(ApplicationComposition composition) {
        return new CreateStrategyHttpHandler(
                new CreateStrategyHttpHandler.CreateStrategyHttpHandlerDeps(
                        composition.strategyPublisher()));
    }

    private static HttpHandler listStrategiesHandler(ApplicationComposition composition) {
        return new ListStrategiesHttpHandler(
                new ListStrategiesHttpHandler.ListStrategiesHttpHandlerDeps(
                        composition.strategyPublisher()));
    }

    private static HttpHandler getStrategyHandler(ApplicationComposition composition) {
        return new PathIdStrategyHttpHandler(
                new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                        composition.strategyPublisher(),
                        PathIdStrategyHttpHandler.PathIdEventKind.GET));
    }

    private static HttpHandler updateStrategyHandler(ApplicationComposition composition) {
        return new UpdateStrategyHttpHandler(
                new UpdateStrategyHttpHandler.UpdateStrategyHttpHandlerDeps(
                        composition.strategyPublisher()));
    }

    private static HttpHandler deleteStrategyHandler(ApplicationComposition composition) {
        return new PathIdStrategyHttpHandler(
                new PathIdStrategyHttpHandler.PathIdStrategyHttpHandlerDeps(
                        composition.strategyPublisher(),
                        PathIdStrategyHttpHandler.PathIdEventKind.DELETE));
    }
}
