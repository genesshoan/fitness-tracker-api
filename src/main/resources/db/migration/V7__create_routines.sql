CREATE TABLE routines
(
    id          UUID                        NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    name        VARCHAR(255)                NOT NULL,
    description TEXT,
    active      BOOLEAN                     NOT NULL DEFAULT TRUE,
    user_id     UUID                        NOT NULL,

    CONSTRAINT pk_routines PRIMARY KEY (id),
    CONSTRAINT fk_routines_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_routines_name_user_id UNIQUE (name, user_id)
);
