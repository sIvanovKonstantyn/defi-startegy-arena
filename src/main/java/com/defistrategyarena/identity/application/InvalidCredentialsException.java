package com.defistrategyarena.identity.application;

public final class InvalidCredentialsException extends RuntimeException {

    private static final String MESSAGE = "invalid credentials";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }

    public InvalidCredentialsException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
