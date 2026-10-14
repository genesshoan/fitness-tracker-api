CREATE TABLE verification_codes
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    subject_id  UUID                     NOT NULL,
    hash        BYTEA                    NOT NULL,
    purpose     VARCHAR(255)             NOT NULL,
    attempts    INT                      NOT NULL DEFAULT 0,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_verification_codes PRIMARY KEY (id),
    CONSTRAINT fk_verification_codes_user
        FOREIGN KEY (subject_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_verification_codes_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_verification_codes_user_purpose
    ON verification_codes (subject_id, purpose, expires_at);
