package com.defistrategyarena.strategy.adapter.persistence;

import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;

enum DuplicateKeyMapper {
    ;

    private static final String UNIQUE_VIOLATION_STATE = "23505";

    static DuplicateStrategyException map(DataAccessException ex) {
        if (isUniqueViolation(ex)) {
            return new DuplicateStrategyException(ex);
        }
        throw ex;
    }

    private static boolean isUniqueViolation(DataAccessException ex) {
        return ex instanceof IntegrityConstraintViolationException
                || UNIQUE_VIOLATION_STATE.equals(ex.sqlState())
                || hasUniqueSqlState(ex);
    }

    private static boolean hasUniqueSqlState(DataAccessException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (isUniqueSqlException(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static boolean isUniqueSqlException(Throwable cause) {
        return cause instanceof java.sql.SQLException sqlException
                && UNIQUE_VIOLATION_STATE.equals(sqlException.getSQLState());
    }
}
