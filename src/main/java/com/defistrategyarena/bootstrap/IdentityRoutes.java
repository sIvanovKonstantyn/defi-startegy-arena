package com.defistrategyarena.bootstrap;

import com.defistrategyarena.shared.infra.http.HttpHandler;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistration;
import com.defistrategyarena.shared.infra.http.HttpRouteRegistry;

enum IdentityRoutes {
    ;

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String PATH_AUTH_SIGNUP = "/auth/signup";
    private static final String PATH_AUTH_LOGIN = "/auth/login";
    private static final String PATH_AUTH_LOGOUT = "/auth/logout";
    private static final String PATH_AUTH_ME = "/auth/me";

    static void register(RouteRegistrationTarget target) {
        HttpRouteRegistry routes = target.routes();
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_POST, PATH_AUTH_SIGNUP, signupHandler(target))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_POST, PATH_AUTH_LOGIN, loginHandler(target))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(
                                METHOD_POST, PATH_AUTH_LOGOUT, logoutHandler(target))));
        routes.register(
                HttpRouteRegistration.create(
                        new HttpRouteRegistration(METHOD_GET, PATH_AUTH_ME, meHandler(target))));
    }

    private static HttpHandler signupHandler(RouteRegistrationTarget target) {
        return new SignupHttpHandler(
                new SignupHttpHandler.SignupHttpHandlerDeps(target.composition().identityHttp()));
    }

    private static HttpHandler loginHandler(RouteRegistrationTarget target) {
        return new LoginHttpHandler(
                new LoginHttpHandler.LoginHttpHandlerDeps(target.composition().identityHttp()));
    }

    private static HttpHandler logoutHandler(RouteRegistrationTarget target) {
        return new LogoutHttpHandler(
                new LogoutHttpHandler.LogoutHttpHandlerDeps(target.composition().identityHttp()));
    }

    private static HttpHandler meHandler(RouteRegistrationTarget target) {
        return new MeHttpHandler(
                new MeHttpHandler.MeHttpHandlerDeps(target.composition().identityHttp()));
    }

    record RouteRegistrationTarget(HttpRouteRegistry routes, ApplicationComposition composition) {}
}
