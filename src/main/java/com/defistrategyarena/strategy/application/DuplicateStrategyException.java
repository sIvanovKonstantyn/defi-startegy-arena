package com.defistrategyarena.strategy.application;

public final class DuplicateStrategyException extends RuntimeException {

    private static final String MESSAGE = "strategy already exists for owner and name";

    public DuplicateStrategyException() {
        super(MESSAGE);
    }
}
