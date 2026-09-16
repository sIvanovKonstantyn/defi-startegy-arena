package com.defistrategyarena.shared.infra.messaging;

import java.util.UUID;
import org.jooq.Field;
import org.jooq.impl.DSL;

enum MessagingEventColumns {
    ;

    static final String COL_EVENT_TYPE = "event_type";
    static final String COL_PAYLOAD_JSON = "payload_json";
    static final String COL_CORRELATION_ID = "correlation_id";

    static final Field<String> EVENT_TYPE = DSL.field(DSL.name(COL_EVENT_TYPE), String.class);
    static final Field<String> PAYLOAD_JSON = DSL.field(DSL.name(COL_PAYLOAD_JSON), String.class);
    static final Field<String> CORRELATION_ID =
            DSL.field(DSL.name(COL_CORRELATION_ID), String.class);

    static Field<UUID> uuidColumn(ColumnName name) {
        return DSL.field(DSL.name(name.value()), UUID.class);
    }

    record ColumnName(String value) {}
}
