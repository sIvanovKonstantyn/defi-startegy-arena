CREATE TABLE messaging_outbox (
    outbox_id UUID NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    payload_json VARCHAR(16384) NOT NULL,
    correlation_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT messaging_outbox_pkey PRIMARY KEY (outbox_id)
);

CREATE INDEX messaging_outbox_status_created_idx ON messaging_outbox (status, created_at);

CREATE TABLE messaging_inbox (
    event_id UUID NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    payload_json VARCHAR(16384) NOT NULL,
    correlation_id VARCHAR(128),
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT messaging_inbox_pkey PRIMARY KEY (event_id)
);
