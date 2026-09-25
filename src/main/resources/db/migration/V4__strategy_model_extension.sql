-- Phase 10: normalized rules/conditions + indicator catalog; retire definition_json.

CREATE TABLE indicators (
    indicator_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    parameter_schema_json VARCHAR(2048) NOT NULL,
    calculation_rule_json VARCHAR(8192),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT indicators_pkey PRIMARY KEY (indicator_id),
    CONSTRAINT indicators_code_uq UNIQUE (code)
);

INSERT INTO indicators (indicator_id, code, display_name, parameter_schema_json, calculation_rule_json)
VALUES
    ('a0000000-0000-4000-8000-000000000001', 'sma', 'SMA',
     '[{"name":"period","type":"INTEGER","required":true,"defaultValue":"14"}]', NULL),
    ('a0000000-0000-4000-8000-000000000002', 'ema', 'EMA',
     '[{"name":"period","type":"INTEGER","required":true,"defaultValue":"14"}]', NULL),
    ('a0000000-0000-4000-8000-000000000003', 'rsi', 'RSI',
     '[{"name":"period","type":"INTEGER","required":true,"defaultValue":"14"}]', NULL),
    ('a0000000-0000-4000-8000-000000000004', 'bollinger_bands', 'Bollinger Bands',
     '[{"name":"period","type":"INTEGER","required":true,"defaultValue":"14"},{"name":"stdDev","type":"DECIMAL","required":true,"defaultValue":"2"}]', NULL),
    ('a0000000-0000-4000-8000-000000000005', 'macd', 'MACD',
     '[{"name":"fast","type":"INTEGER","required":true,"defaultValue":"12"},{"name":"slow","type":"INTEGER","required":true,"defaultValue":"26"},{"name":"signal","type":"INTEGER","required":true,"defaultValue":"9"}]', NULL);

ALTER TABLE strategies ADD COLUMN description VARCHAR(2048) NOT NULL DEFAULT '';

CREATE TABLE strategy_rules (
    rule_row_id UUID NOT NULL,
    strategy_id UUID NOT NULL,
    rule_key VARCHAR(128) NOT NULL,
    sort_order INTEGER NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    action_params_json VARCHAR(2048) NOT NULL,
    CONSTRAINT strategy_rules_pkey PRIMARY KEY (rule_row_id),
    CONSTRAINT strategy_rules_strategy_fk FOREIGN KEY (strategy_id) REFERENCES strategies (strategy_id) ON DELETE CASCADE
);

CREATE INDEX strategy_rules_strategy_id_idx ON strategy_rules (strategy_id);

CREATE TABLE strategy_rule_conditions (
    condition_id UUID NOT NULL,
    rule_row_id UUID NOT NULL,
    parent_condition_id UUID,
    sort_order INTEGER NOT NULL,
    node_type VARCHAR(64) NOT NULL,
    operator VARCHAR(16),
    instrument VARCHAR(128),
    threshold VARCHAR(128),
    indicator_id UUID,
    parameters_json VARCHAR(2048),
    CONSTRAINT strategy_rule_conditions_pkey PRIMARY KEY (condition_id),
    CONSTRAINT strategy_rule_conditions_rule_fk FOREIGN KEY (rule_row_id) REFERENCES strategy_rules (rule_row_id) ON DELETE CASCADE,
    CONSTRAINT strategy_rule_conditions_parent_fk FOREIGN KEY (parent_condition_id) REFERENCES strategy_rule_conditions (condition_id) ON DELETE CASCADE,
    CONSTRAINT strategy_rule_conditions_indicator_fk FOREIGN KEY (indicator_id) REFERENCES indicators (indicator_id)
);

CREATE INDEX strategy_rule_conditions_rule_id_idx ON strategy_rule_conditions (rule_row_id);

DELETE FROM strategies;

ALTER TABLE strategies DROP COLUMN definition_json;
