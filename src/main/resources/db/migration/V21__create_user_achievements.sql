CREATE TABLE user_achievements
(
    id                        UUID                     NOT NULL DEFAULT uuidv7(),
    user_id                   UUID                     NOT NULL,
    achievement_definition_id UUID                     NOT NULL,
    exercise_id               UUID,
    progress_value            DOUBLE PRECISION,
    unlocked                  BOOLEAN                  NOT NULL DEFAULT FALSE,
    unlocked_at               TIMESTAMP WITH TIME ZONE,
    source_session_id         UUID,
    source_set_id             UUID,
    seen                      BOOLEAN                  NOT NULL DEFAULT FALSE,
    seen_at                   TIMESTAMP WITH TIME ZONE,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_user_achievements PRIMARY KEY (id),
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_definition
        FOREIGN KEY (achievement_definition_id) REFERENCES achievement_definitions (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_session FOREIGN KEY (source_session_id) REFERENCES workout_sessions (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_user_achievements_set FOREIGN KEY (source_set_id) REFERENCES session_sets (id)
        ON DELETE SET NULL,

    -- An unlocked achievement has the instant of the unlock
    CONSTRAINT ck_user_achievements_unlocked
        CHECK ((unlocked = FALSE AND unlocked_at IS NULL)
            OR (unlocked = TRUE AND unlocked_at IS NOT NULL))
);

-- One achievement per user, definition, and scope. The exercise is null for a
-- global achievement and references the exercise for the scoped ones.
-- NULLS NOT DISTINCT makes the null exercise unique as well.
CREATE UNIQUE INDEX uk_user_achievements_identity
    ON user_achievements (user_id, achievement_definition_id, exercise_id) NULLS NOT DISTINCT;

CREATE INDEX idx_user_achievements_feed
    ON user_achievements (user_id, unlocked_at DESC);
CREATE INDEX idx_user_achievements_unseen
    ON user_achievements (user_id, seen);
