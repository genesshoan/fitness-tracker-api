CREATE TABLE verification_tokens
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    subject_id  UUID                     NOT NULL,
    hash        BYTEA                    NOT NULL,
    purpose     VARCHAR(255)             NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at     TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_verification_tokens_user
        FOREIGN KEY (subject_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_verification_tokens_user_purpose
    ON verification_tokens (subject_id, purpose, expires_at);
