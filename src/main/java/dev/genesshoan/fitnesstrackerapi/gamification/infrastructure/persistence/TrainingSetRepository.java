package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.persistence.projection.TrainingSetProjection;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionStatus;
import lombok.RequiredArgsConstructor;

/**
 * Reads the training data the achievement evaluation needs.
 *
 * <p>The queries live in this module instead of reusing the workout entities, so
 * the gamification module does not depend on the entities of the workout
 * module. The references between the two are plain identifiers.</p>
 */
@Repository
@RequiredArgsConstructor
public class TrainingSetRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Loads the completed sets of a session, in the order they were performed.
     *
     * @param sessionId the session to read
     * @return the completed sets, ordered by exercise position and set number
     */
    public List<TrainingSetProjection> findCompletedSetsOfSession(UUID sessionId) {

        String sql = """
                SELECT
                    ss.id AS set_id,
                    se.exercise_id,
                    ws.id AS session_id,
                    ss.reps,
                    ss.weight_kg,
                    ss.duration_seconds,
                    ss.distance_km,
                    COALESCE(ws.completed_at, ws.started_at) AS performed_at
                FROM session_sets ss
                JOIN session_exercises se ON ss.session_exercise_id = se.id
                JOIN workout_sessions ws ON se.session_id = ws.id
                WHERE se.session_id = :sessionId
                    AND ss.completed = true
                ORDER BY se.position ASC, ss.set_number ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("sessionId", sessionId);

        return jdbcTemplate.query(sql, params, TrainingSetRepository::mapRow);
    }

    /**
     * Loads one completed set of a user.
     *
     * @param setId the set to read
     * @param userId the owner of the session
     * @return the set, or {@link Optional#empty()} if the set does not exist,
     *         does not belong to the user, or is not completed
     */
    public Optional<TrainingSetProjection> findCompletedSet(UUID setId, UUID userId) {

        String sql = """
                SELECT
                    ss.id AS set_id,
                    se.exercise_id,
                    ws.id AS session_id,
                    ss.reps,
                    ss.weight_kg,
                    ss.duration_seconds,
                    ss.distance_km,
                    COALESCE(ws.completed_at, ws.started_at) AS performed_at
                FROM session_sets ss
                JOIN session_exercises se ON ss.session_exercise_id = se.id
                JOIN workout_sessions ws ON se.session_id = ws.id
                WHERE ss.id = :setId
                    AND ws.user_id = :userId
                    AND ss.completed = true
                """;

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("setId", setId).addValue("userId", userId);

        return jdbcTemplate.query(sql, params, TrainingSetRepository::mapRow).stream()
                .findFirst();
    }

    /**
     * Loads every completed set a user performed on one exercise, oldest first.
     *
     * <p>Only completed sessions are considered, because a record set during a
     * session that was never finished is not a record the user keeps.</p>
     *
     * @param userId the owner of the sessions
     * @param exerciseId the exercise to read
     * @return the completed sets of the exercise, oldest first
     */
    public List<TrainingSetProjection> findCompletedSetsOfExercise(UUID userId, UUID exerciseId) {

        String sql = """
                SELECT
                    ss.id AS set_id,
                    se.exercise_id,
                    ws.id AS session_id,
                    ss.reps,
                    ss.weight_kg,
                    ss.duration_seconds,
                    ss.distance_km,
                    COALESCE(ws.completed_at, ws.started_at) AS performed_at
                FROM session_sets ss
                JOIN session_exercises se ON ss.session_exercise_id = se.id
                JOIN workout_sessions ws ON se.session_id = ws.id
                WHERE se.exercise_id = :exerciseId
                    AND ws.user_id = :userId
                    AND ws.status = :completedStatus
                    AND ss.completed = true
                ORDER BY ws.completed_at ASC, se.position ASC, ss.set_number ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("exerciseId", exerciseId)
                .addValue("completedStatus", SessionStatus.COMPLETED.name());

        return jdbcTemplate.query(sql, params, TrainingSetRepository::mapRow);
    }

    private static TrainingSetProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp performedAt = rs.getTimestamp("performed_at");
        return new TrainingSetProjection(
                rs.getObject("set_id", UUID.class),
                rs.getObject("exercise_id", UUID.class),
                rs.getObject("session_id", UUID.class),
                (Integer) rs.getObject("reps"),
                (Double) rs.getObject("weight_kg"),
                (Integer) rs.getObject("duration_seconds"),
                (Double) rs.getObject("distance_km"),
                performedAt == null ? null : performedAt.toInstant());
    }
}
