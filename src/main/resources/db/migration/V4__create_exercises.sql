CREATE TABLE exercises
(
    id          UUID                        NOT NULL DEFAULT uuidv7(),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    name        VARCHAR(255)                NOT NULL,
    slug        VARCHAR(255)                NOT NULL,
    description TEXT                        NOT NULL,
    category    VARCHAR(255)                NOT NULL,
    difficulty  VARCHAR(255)                NOT NULL,
    active      BOOLEAN                     NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_exercises PRIMARY KEY (id),
    CONSTRAINT uk_exercises_name UNIQUE (name),
    CONSTRAINT uk_exercises_slug UNIQUE (slug),
    CONSTRAINT ck_exercises_category  CHECK (category   IN ('STRENGTH', 'CARDIO', 'MOBILITY')),
    CONSTRAINT ck_exercises_difficulty CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);
