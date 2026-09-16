package com.defistrategyarena.identity.application;

import java.util.Objects;

public record AccessTokenQuery(String accessToken) {

    private static final String TOKEN_REQUIRED = "access token must not be blank";
    private static final String DRAFT_REQUIRED = "access token query must not be null";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final int INDEX_START = 0;

    public AccessTokenQuery {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(TOKEN_REQUIRED);
        }
    }

    public static AccessTokenQuery create(AccessTokenQuery draft) {
        Objects.requireNonNull(draft, DRAFT_REQUIRED);
        return new AccessTokenQuery(draft.accessToken());
    }

    public static AccessTokenQuery fromAuthorizationHeader(AuthorizationHeader header) {
        Objects.requireNonNull(header);
        return new AccessTokenQuery(extractBearer(header));
    }

    private static String extractBearer(AuthorizationHeader header) {
        String value = header.value();
        requirePresent(new HeaderValue(value));
        requireBearerPrefix(new HeaderValue(value));
        String token = value.substring(BEARER_PREFIX_LENGTH).trim();
        requirePresent(new HeaderValue(token));
        return token;
    }

    private static void requirePresent(HeaderValue value) {
        if (value.text() == null || value.text().isBlank()) {
            throw new UnauthorizedException();
        }
    }

    private static void requireBearerPrefix(HeaderValue value) {
        if (!value.text()
                .regionMatches(true, INDEX_START, BEARER_PREFIX, INDEX_START, BEARER_PREFIX_LENGTH)) {
            throw new UnauthorizedException();
        }
    }

    public record AuthorizationHeader(String value) {}

    private record HeaderValue(String text) {}
}
