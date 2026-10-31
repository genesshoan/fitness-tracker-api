CREATE TABLE personal_records
(
    id          UUID                     NOT NULL DEFAULT uuidv7(),
    user_id     UUID                     NOT NULL,
    exercise_id UUID                     NOT NULL,
    metric      VARCHAR(32)              NOT NULL,
    weight_kg   DOUBLE PRECISION,
    value       DOUBLE PRECISION         NOT NULL,
    set_id      UUID,
    session_id  UUID,
    achieved_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_personal_records PRIMARY KEY (id),
    CONSTRAINT fk_personal_records_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_personal_records_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_personal_records_set FOREIGN KEY (set_id) REFERENCES session_sets (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_personal_records_session FOREIGN KEY (session_id) REFERENCES workout_sessions (id)
        ON DELETE SET NULL,

    CONSTRAINT ck_personal_records_metric
        CHECK (metric IN ('MAX_WEIGHT', 'MAX_ESTIMATED_1RM', 'MAX_REPS_AT_WEIGHT',
            'MAX_DISTANCE', 'MAX_DURATION')),
    CONSTRAINT ck_personal_records_value CHECK (value > 0),

    -- The record of repetitions belongs to a weight; the other metrics do not use one
    CONSTRAINT ck_personal_records_weight CHECK (
        (metric = 'MAX_REPS_AT_WEIGHT' AND weight_kg IS NOT NULL AND weight_kg > 0)
            OR (metric <> 'MAX_REPS_AT_WEIGHT' AND weight_kg IS NULL))
);

-- One row per metric and exercise; the record of repetitions, one per weight.
-- NULLS NOT DISTINCT makes the null weight of the other metrics unique as well.
CREATE UNIQUE INDEX uk_personal_records_identity
    ON personal_records (user_id, exercise_id, metric, weight_kg) NULLS NOT DISTINCT;

CREATE INDEX idx_personal_records_user_exercise ON personal_records (user_id, exercise_id);
