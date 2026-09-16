package com.defistrategyarena.identity.application;

public final class DuplicateEmailException extends RuntimeException {

    private static final String MESSAGE = "email already registered";

    public DuplicateEmailException() {
        super(MESSAGE);
    }

    public DuplicateEmailException(Throwable cause) {
        super(MESSAGE, cause);
    }
}
