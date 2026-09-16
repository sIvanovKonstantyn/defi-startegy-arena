package com.defistrategyarena.shared.messaging;

public final class ImmediateTransactionRunner implements TransactionRunner {

    @Override
    public void run(UnitOfWork work) {
        work.execute();
    }
}
