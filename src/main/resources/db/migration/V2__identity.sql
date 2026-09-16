CREATE TABLE users (
    user_id UUID NOT NULL,
    email VARCHAR(512) NOT NULL,
    email_normalized VARCHAR(512) NOT NULL,
    display_name VARCHAR(512) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_email_uq UNIQUE (email_normalized)
);

CREATE TABLE user_passwords (
    user_id UUID NOT NULL,
    password_hash VARCHAR(1024) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_passwords_pkey PRIMARY KEY (user_id),
    CONSTRAINT user_passwords_user_fk FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE auth_sessions (
    session_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT auth_sessions_pkey PRIMARY KEY (session_id),
    CONSTRAINT auth_sessions_token_hash_uq UNIQUE (token_hash),
    CONSTRAINT auth_sessions_user_fk FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX auth_sessions_user_id_idx ON auth_sessions (user_id);
