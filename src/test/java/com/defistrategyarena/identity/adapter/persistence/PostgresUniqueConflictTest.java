package com.defistrategyarena.identity.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.defistrategyarena.identity.application.DuplicateEmailException;
import java.sql.SQLException;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;

class PostgresUniqueConflictTest {

    private static final String UNIQUE_STATE = "23505";
    private static final String OTHER_STATE = "23000";

    @Test
    void detects_unique_conflicts_from_exception_shapes() {
        assertTrue(
                PostgresUniqueConflict.matches(
                        new IntegrityConstraintViolationException(
                                "dup", new SQLException("x", UNIQUE_STATE))));
        assertTrue(
                PostgresUniqueConflict.matches(
                        new DataAccessException("wrapped", new SQLException("x", UNIQUE_STATE))));
        assertTrue(
                PostgresUniqueConflict.matches(
                        new DataAccessException("state") {
                            @Override
                            public String sqlState() {
                                return UNIQUE_STATE;
                            }
                        }));
        assertFalse(
                PostgresUniqueConflict.matches(
                        new DataAccessException("other", new SQLException("x", OTHER_STATE))));
        assertFalse(PostgresUniqueConflict.matches(new DataAccessException("bare")));
        assertFalse(
                PostgresUniqueConflict.matches(
                        new DataAccessException("nested", new IllegalStateException("no-sql"))));
    }

    @Test
    void propagate_maps_unique_and_rethrows_other_failures() {
        assertInstanceOf(
                DuplicateEmailException.class,
                JooqUserWrites.mappedPersistFailure(
                        new IntegrityConstraintViolationException(
                                "dup", new SQLException("x", UNIQUE_STATE))));
        assertInstanceOf(
                DataAccessException.class,
                JooqUserWrites.mappedPersistFailure(
                        new DataAccessException("other", new SQLException("x", OTHER_STATE))));
    }

    @Test
    void detects_unique_conflict_via_cause_when_sql_state_absent() {
        DataAccessException failure =
                new DataAccessException("wrapped", new SQLException("x", UNIQUE_STATE)) {
                    @Override
                    public String sqlState() {
                        return null;
                    }
                };
        assertTrue(PostgresUniqueConflict.matches(failure));
    }
}
