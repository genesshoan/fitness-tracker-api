CREATE INDEX idx_workout_sessions_user_completed_at
    ON workout_sessions (user_id, completed_at)
    WHERE status = 'COMPLETED';

CREATE INDEX idx_session_exercises_session_exercise
    ON session_exercises (session_id, exercise_id);

CREATE INDEX idx_session_sets_session_exercise_completed
    ON session_sets (session_exercise_id, completed);
