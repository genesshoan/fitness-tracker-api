CREATE TABLE users
(
    id            UUID                        NOT NULL DEFAULT uuidv7(),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    username      VARCHAR(255)                NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    role          VARCHAR(255)                NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_username UNIQUE (username)
);
