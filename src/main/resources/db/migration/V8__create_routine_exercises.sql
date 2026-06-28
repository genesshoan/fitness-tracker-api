CREATE TABLE routine_exercises
(
    routine_id                  UUID NOT NULL,
    exercise_id                 UUID NOT NULL,
    position                    INT  NOT NULL,
    default_sets                INT  NOT NULL,
    default_reps                INT,
    default_weight_kg           INT,
    default_duration_seconds    INT,
    default_distance_meters     INT,
    notes             TEXT,

    CONSTRAINT pk_routine_exercises PRIMARY KEY (routine_id, exercise_id),
    CONSTRAINT fk_routine_exercises_routine FOREIGN KEY (routine_id) REFERENCES routines(id),
    CONSTRAINT fk_routine_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises(id),
    CONSTRAINT uk_routine_exercise_position UNIQUE (routine_id, position),

    CONSTRAINT ck_routine_exercises_default_duration_seconds CHECK (default_duration_seconds IS NULL OR default_duration_seconds >= 0),
    CONSTRAINT ck_routine_exercises_default_distance_meters CHECK (default_distance_meters IS NULL OR default_distance_meters >= 0),
    CONSTRAINT ck_routine_exercises_default_weight_kg CHECK (default_weight_kg IS NULL OR default_weight_kg >= 0),
    CONSTRAINT ck_routine_exercises_default_sets CHECK (default_sets >= 0),
    CONSTRAINT ck_routine_exercises_default_reps CHECK (default_reps IS NULL OR default_reps >= 0)
);
