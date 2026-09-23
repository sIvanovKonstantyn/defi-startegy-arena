package com.defistrategyarena.identity.adapter.persistence;

import java.sql.SQLException;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;

final class PostgresUniqueConflict {

    private static final String UNIQUE_STATE = "23505";

    private PostgresUniqueConflict() {}

    static boolean matches(DataAccessException failure) {
        return failure instanceof IntegrityConstraintViolationException
                || UNIQUE_STATE.equals(failure.sqlState())
                || causeChainHasUniqueState(new CauseRoot(failure.getCause()));
    }

    private static boolean causeChainHasUniqueState(CauseRoot root) {
        Throwable cursor = root.throwable();
        while (cursor != null) {
            if (cursor instanceof SQLException sqlException
                    && UNIQUE_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private record CauseRoot(Throwable throwable) {}
}
