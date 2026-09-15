package com.defistrategyarena.shared.infra.http;

public record HttpResponse(int status, String contentType, String body) {

    private static final String CONTENT_TYPE_REQUIRED = "content type must not be blank";

    public HttpResponse {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(CONTENT_TYPE_REQUIRED);
        }
        if (body == null) {
            body = "";
        }
    }

    public static HttpResponse create(HttpResponse draft) {
        return new HttpResponse(draft.status(), draft.contentType(), draft.body());
    }
}
