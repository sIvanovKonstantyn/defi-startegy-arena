package com.defistrategyarena.bootstrap;

enum AcceptedResponses {
    ;

    private static final int STATUS_BAD_REQUEST = 400;

    static AcceptedHttpResponse badRequest() {
        return AcceptedHttpResponse.empty(new AcceptedHttpResponse.StatusCode(STATUS_BAD_REQUEST));
    }
}
