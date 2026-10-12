CREATE TABLE verification_codes
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    user_id     UUID                     NOT NULL,
    code_hash   VARCHAR(255)             NOT NULL,
    type        VARCHAR(255)             NOT NULL,
    attempts    INT                      NOT NULL DEFAULT 0,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_verification_codes PRIMARY KEY (id),
    CONSTRAINT fk_verification_codes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_verification_codes_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_verification_codes_user_type
    ON verification_codes (user_id, type, expires_at);
