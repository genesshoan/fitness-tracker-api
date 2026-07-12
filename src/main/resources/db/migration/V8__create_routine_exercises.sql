CREATE TABLE routine_exercises
(
    id                  UUID NOT NULL DEFAULT uuidv7(),
    routine_id                  UUID NOT NULL,
    exercise_id                 UUID NOT NULL,
    position                    INT  NOT NULL,
    default_rest_seconds        INT NOT NULL,
    default_sets                INT  NOT NULL,
    default_reps                INT,
    default_weight_kg           DOUBLE PRECISION,
    default_duration_seconds    INT,
    default_distance_km         DOUBLE PRECISION,
    notes             TEXT,

    CONSTRAINT pk_routine_exercises PRIMARY KEY (id),
    CONSTRAINT fk_routine_exercises_routine FOREIGN KEY (routine_id) REFERENCES routines(id)
      ON DELETE CASCADE,
    CONSTRAINT fk_routine_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id),
    CONSTRAINT uk_routine_exercise_position UNIQUE (routine_id, position),

    CONSTRAINT ck_routine_exercises_default_duration_seconds CHECK (default_duration_seconds IS NULL OR default_duration_seconds >= 0),
    CONSTRAINT ck_routine_exercises_default_distance_km CHECK (default_distance_km IS NULL OR default_distance_km >= 0),
    CONSTRAINT ck_routine_exercises_default_weight_kg CHECK (default_weight_kg IS NULL OR default_weight_kg >= 0),
    CONSTRAINT ck_routine_exercises_default_sets CHECK (default_sets > 0),
    CONSTRAINT ck_routine_exercises_default_reps CHECK (default_reps IS NULL OR default_reps >= 0),
    CONSTRAINT ck_routine_exercises_default_rest_seconds CHECK (default_rest_seconds >= 0)
);
