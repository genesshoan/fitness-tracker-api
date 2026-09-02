CREATE TABLE session_exercises
(
    id          UUID NOT NULL DEFAULT uuidv7(),
    session_id  UUID NOT NULL,
    exercise_id UUID NOT NULL,
    position    INT NOT NULL,
    notes       TEXT,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_session_exercises PRIMARY KEY(id),
    CONSTRAINT fk_session_exercises_session_workout FOREIGN KEY(session_id) REFERENCES workout_sessions(id),
    CONSTRAINT fk_session_exercises_exercise FOREIGN KEY(exercise_id) REFERENCES exercises(id),
    CONSTRAINT uk_session_exercises_session_id_position UNIQUE (session_id, position) 
        DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_session_exercises_position CHECK (position > 0)
)
