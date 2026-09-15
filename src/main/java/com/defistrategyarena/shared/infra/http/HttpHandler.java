package com.defistrategyarena.shared.infra.http;

@FunctionalInterface
public interface HttpHandler {

    HttpResponse handle(HttpRequest request);
}
