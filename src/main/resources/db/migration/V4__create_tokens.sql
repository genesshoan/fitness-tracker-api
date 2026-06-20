CREATE TABLE tokens
(
    jti        UUID    NOT NULL,
    family_id  UUID    NOT NULL,
    revoked    BOOLEAN NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id    BIGINT  NOT NULL,
    CONSTRAINT pk_tokens PRIMARY KEY (jti)
);

ALTER TABLE tokens
    ADD CONSTRAINT FK_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);