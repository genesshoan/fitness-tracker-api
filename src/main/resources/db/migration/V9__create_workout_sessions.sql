CREATE TABLE workout_sessions 
(
    id           UUID NOT NULL DEFAULT uuidv7(),
    user_id      UUID NOT NULL,
    routine_id   UUID,
    status       VARCHAR NOT NULL DEFAULT 'IN_PROGRESS',
    completed_at TIMESTAMP,
    notes        TEXT,
    version      INT,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_workout_sessions PRIMARY KEY (id),
    CONSTRAINT fk_workout_sessions_user FOREIGN KEY(user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_workout_sessions_routine FOREIGN KEY(routine_id) REFERENCES routines(id)
        ON DELETE SET NULL,

    CONSTRAINT ck_workout_sessions_status CHECK (status IN ('COMPLETED', 'IN_PROGRESS', 'CANCELLED'))
);
