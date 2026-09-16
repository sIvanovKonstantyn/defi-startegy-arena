package com.defistrategyarena.identity.application;

public final class UnauthorizedException extends RuntimeException {

    private static final String MESSAGE = "unauthorized";

    public UnauthorizedException() {
        super(MESSAGE);
    }

    public UnauthorizedException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
