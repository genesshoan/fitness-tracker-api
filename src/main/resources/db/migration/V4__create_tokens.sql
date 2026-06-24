CREATE TABLE tokens
(
    jti        UUID                        NOT NULL DEFAULT uuidv7(),
    family_id  UUID                        NOT NULL,
    revoked    BOOLEAN                     NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    UUID                        NOT NULL,

    CONSTRAINT pk_tokens PRIMARY KEY (jti),
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
