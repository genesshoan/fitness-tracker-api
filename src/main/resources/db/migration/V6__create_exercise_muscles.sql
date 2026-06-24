CREATE TABLE exercise_muscles
(
    exercise_id  UUID         NOT NULL DEFAULT uuidv7(),
    muscle_id    UUID         NOT NULL,
    impact_level VARCHAR(255) NOT NULL,

    CONSTRAINT pk_exercise_muscles PRIMARY KEY (exercise_id, muscle_id),
    CONSTRAINT fk_exercise_muscles_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT fk_exercise_muscles_muscle   FOREIGN KEY (muscle_id)   REFERENCES muscles  (id) ON DELETE CASCADE,
    CONSTRAINT ck_exercise_muscles_impact_level CHECK (impact_level IN ('PRIMARY', 'SECONDARY', 'STABILIZER'))
);
