CREATE TABLE strategies (
    strategy_id UUID NOT NULL,
    owner_id VARCHAR(512) NOT NULL,
    owner_id_normalized VARCHAR(512) NOT NULL,
    name VARCHAR(512) NOT NULL,
    name_normalized VARCHAR(512) NOT NULL,
    privacy VARCHAR(32) NOT NULL,
    version_number INTEGER NOT NULL,
    definition_json VARCHAR(8192) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT strategies_pkey PRIMARY KEY (strategy_id),
    CONSTRAINT strategies_owner_name_uq UNIQUE (owner_id_normalized, name_normalized)
);

CREATE INDEX strategies_owner_id_idx ON strategies (owner_id);
