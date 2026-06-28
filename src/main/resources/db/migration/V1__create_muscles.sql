CREATE TABLE muscles
(
    id          UUID                        NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    name        VARCHAR(255)                NOT NULL,
    slug        VARCHAR(255)                NOT NULL,
    body_region VARCHAR(255)                NOT NULL,

    CONSTRAINT pk_muscles PRIMARY KEY (id),
    CONSTRAINT uc_muscles_name UNIQUE (name),
    CONSTRAINT uc_muscles_slug UNIQUE (slug)
);
