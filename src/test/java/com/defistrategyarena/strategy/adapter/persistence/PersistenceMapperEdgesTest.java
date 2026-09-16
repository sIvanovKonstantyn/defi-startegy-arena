package com.defistrategyarena.strategy.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.defistrategyarena.strategy.application.DuplicateStrategyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;

class PersistenceMapperEdgesTest {

    private static final String UNIQUE_STATE = "23505";
    private static final String OTHER_STATE = "42000";
    private static final String FAILURE_MESSAGE = "failed";

    @Test
    void duplicate_key_mapper_handles_integrity_sqlstate_and_cause() {
        IntegrityConstraintViolationException integrity =
                new IntegrityConstraintViolationException("dup");
        assertInstanceOf(DuplicateStrategyException.class, DuplicateKeyMapper.map(integrity));

        DataAccessException withState =
                new DataAccessException("unique", new SQLException("x", UNIQUE_STATE));
        assertInstanceOf(DuplicateStrategyException.class, DuplicateKeyMapper.map(withState));

        DataAccessException withCause =
                new DataAccessException(
                        "wrapped",
                        new IllegalStateException(
                                new SQLException(
                                        "outer",
                                        OTHER_STATE,
                                        new SQLException("inner", UNIQUE_STATE))));
        assertInstanceOf(DuplicateStrategyException.class, DuplicateKeyMapper.map(withCause));

        DataAccessException other =
                new DataAccessException(
                        "other", new IllegalStateException(new SQLException("x", OTHER_STATE)));
        assertThrows(DataAccessException.class, () -> DuplicateKeyMapper.map(other));
    }

    @Test
    void json_write_failure_is_wrapped() {
        ObjectMapper mapper =
                new ObjectMapper() {
                    @Override
                    public String writeValueAsString(Object value) throws JsonProcessingException {
                        throw new JsonMappingException((com.fasterxml.jackson.core.JsonParser) null, "boom");
                    }
                };
        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                StrategyDefinitionJsonCodec.writeJson(
                                        new StrategyDefinitionJsonCodec.JsonWriteRequest(
                                                mapper, "value", FAILURE_MESSAGE)));
        assertEquals(FAILURE_MESSAGE, ex.getMessage());
        assertInstanceOf(JsonProcessingException.class, ex.getCause());
    }
}
