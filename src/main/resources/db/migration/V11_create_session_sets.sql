CREATE TABLE session_sets
(
    id UUID NOT NULL DEFAULT uuidv7(),
    session_exercise_id UUID NOT NULL,
    set_number INT NOT NULL,
    reps INT,
    weight_kg DOUBLE PRECISION,
    duration_seconds INT,
    distance_km DOUBLE PRECISION,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_session_sets PRIMARY KEY(id),
    CONSTRAINT fk_session_sets_session_exercises FOREIGN KEY (session_exercise_id) REFERENCES session_exercises(id)
        ON DELETE CASCADE,
    CONSTRAINT uk_session_sets_session_exercise_set_number UNIQUE (session_exercise_id, set_number),
    CONSTRAINT uk_session_sets_reps CHECK (reps IS NULL OR reps > 0),
    CONSTRAINT ck_session_sets_distance_km CHECK (distance_km IS NULL OR distance_km >= 0),
    CONSTRAINT ck_session_sets_weight_kg CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT ck_session_sets_duration_seconds CHECK (duration_seconds IS NULL OR duration_seconds >= 0)
)
