package com.defistrategyarena.shared.messaging;

public interface TransactionRunner {

    void run(UnitOfWork work);

    @FunctionalInterface
    interface UnitOfWork {
        void execute();
    }
}
