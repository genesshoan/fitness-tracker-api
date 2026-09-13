package dev.genesshoan.fitnesstrackerapi.stats.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.ExerciseProgressProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.OneRepMaxProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.RankedSetProjection;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StatsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Map<UUID, List<RankedSetProjection>> findRankedSets(
            UUID userId, Set<UUID> exerciseIds, Instant beforeStartedAt) {

        if (exerciseIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                WITH base AS (
                    SELECT ss.id, ss.weight_kg, ss.reps, ss.distance_km, ss.duration_seconds,
                        se.exercise_id, ws.id AS session_id
                    FROM session_sets ss
                    JOIN session_exercises se ON ss.session_exercise_id = se.id
                    JOIN workout_sessions ws ON se.session_id = ws.id
                    WHERE ws.user_id = :userId
                        AND se.exercise_id IN (:exerciseIds)
                        AND ss.completed = true
                        AND ws.started_at < :beforeStartedAt
                ),
                ranked AS (
                    SELECT *,
                        ROW_NUMBER() OVER (PARTITION BY exercise_id ORDER BY weight_kg DESC NULLS LAST) AS rn_weight,
                        ROW_NUMBER() OVER (PARTITION BY exercise_id ORDER BY weight_kg * (1 + reps / 30.0) DESC NULLS LAST) AS rn_1rm,
                        ROW_NUMBER() OVER (PARTITION BY exercise_id ORDER BY distance_km DESC NULLS LAST) AS rn_distance,
                        ROW_NUMBER() OVER (PARTITION BY exercise_id ORDER BY duration_seconds DESC NULLS LAST) AS rn_duration,
                        ROW_NUMBER() OVER (PARTITION BY exercise_id, weight_kg ORDER BY reps DESC NULLS LAST) AS rn_reps_per_weight
                    FROM base
                )
                SELECT * FROM ranked
                WHERE rn_weight = 1 OR rn_1rm = 1 OR rn_distance = 1 OR rn_duration = 1 OR rn_reps_per_weight = 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("exerciseIds", exerciseIds)
                .addValue("beforeStartedAt", Timestamp.from(beforeStartedAt));

        List<RankedSetProjection> results = jdbcTemplate.query(sql, params, this::mapRow);

        return results.stream().collect(Collectors.groupingBy(RankedSetProjection::exerciseId));
    }

    public List<Instant> getTrainedDatesBeforeAsc(UUID userId, Instant beforeCompletedAt) {

        String sql = """
                SELECT DISTINCT ws.completed_at
                FROM workout_sessions ws
                WHERE ws.user_id = :userId
                    AND ws.completed_at < :beforeCompletedAt
                ORDER BY ws.completed_at ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("beforeCompletedAt", Timestamp.from(beforeCompletedAt));

        return jdbcTemplate.query(
                sql, params, (rs, rowNum) -> rs.getTimestamp("completed_at").toInstant());
    }

    public List<VolumeSetProjection> getSetsForVolume(UUID sessionId, UUID userId) {

        String sql = """
                SELECT
                    ss.reps,
                    ss.weight_kg
                FROM session_sets ss
                JOIN session_exercises se ON ss.session_exercise_id = se.id
                JOIN workout_sessions ws ON se.session_id = ws.id
                WHERE ss.completed = true
                    AND ss.reps IS NOT NULL
                    AND ss.weight_kg IS NOT NULL
                    AND ws.id = :sessionId
                    AND ws.user_id = :userId
            """;

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("sessionId", sessionId).addValue("userId", userId);

        return jdbcTemplate.query(
                sql, params, (rs, rowNum) -> new VolumeSetProjection(rs.getDouble("weight_kg"), rs.getInt("reps")));
    }

    public Optional<OneRepMaxProjection> getSetForOneRepMax(UUID userId, UUID exerciseId) {

        String sql = """
                SELECT
                    ss.weight_kg * (1 + ss.reps / 30.0) AS estimated_one_rep_max
                FROM session_sets ss
                JOIN session_exercises se ON ss.session_exercise_id = se.id
                JOIN workout_sessions ws ON se.session_id = ws.id
                WHERE se.exercise_id = :exerciseId
                    AND ss.completed = true
                    AND ss.reps IS NOT NULL
                    AND ss.weight_kg IS NOT NULL
                    AND ws.user_id = :userId
                ORDER BY estimated_one_rep_max DESC
                LIMIT 1
            """;

        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("exerciseId", exerciseId).addValue("userId", userId);

        return jdbcTemplate
                .query(sql, params, (rs, rowNum) -> new OneRepMaxProjection(rs.getDouble("estimated_one_rep_max")))
                .stream()
                .findFirst();
    }

    public List<ExerciseProgressProjection> getExerciseProgressPoints(
            UUID userId, UUID exerciseId, Instant from, Instant to) {

        String sql = """
                WITH ranked_sets AS (
                    SELECT
                        ws.completed_at,
                        ss.weight_kg,
                        ss.reps,
                        ss.weight_kg * (1 + ss.reps / 30.0) AS estimated_one_rep_max,
                        ROW_NUMBER() OVER (PARTITION BY ws.id ORDER BY ss.weight_kg * (1 + ss.reps / 30.0) DESC) AS rn
                    FROM session_sets ss
                    JOIN session_exercises se ON ss.session_exercise_id = se.id
                    JOIN workout_sessions ws ON se.session_id = ws.id
                    WHERE se.exercise_id = :exerciseId
                        AND ws.user_id = :userId
                        AND ws.completed_at >= :from
                        AND ws.completed_at < :to
                        AND ss.completed = true
                        AND ss.reps IS NOT NULL
                        AND ss.weight_kg IS NOT NULL
                )
                SELECT
                    completed_at,
                    weight_kg,
                    reps,
                    estimated_one_rep_max
                FROM ranked_sets
                WHERE rn = 1
                ORDER BY completed_at ASC
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("exerciseId", exerciseId)
                .addValue("userId", userId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new ExerciseProgressProjection(
                        rs.getTimestamp("completed_at").toInstant(),
                        rs.getDouble("weight_kg"),
                        rs.getInt("reps"),
                        rs.getDouble("estimated_one_rep_max")));
    }

    private RankedSetProjection mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RankedSetProjection(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("session_id"),
                (UUID) rs.getObject("exercise_id"),
                (Double) rs.getObject("weight_kg"),
                (Integer) rs.getObject("reps"),
                (Double) rs.getObject("distance_km"),
                (Integer) rs.getObject("duration_seconds"),
                toInteger(rs.getObject("rn_weight")),
                toInteger(rs.getObject("rn_1rm")),
                toInteger(rs.getObject("rn_distance")),
                toInteger(rs.getObject("rn_duration")),
                toInteger(rs.getObject("rn_reps_per_weight")));
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }
}
