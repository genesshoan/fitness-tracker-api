package dev.genesshoan.fitnesstrackerapi.routine;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;

/**
 * Repository for {@link Routine} entities.
 * Provides queries for listing routines by user, checking name uniqueness, and loading routines with exercises.
 */
@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    /**
     * Check whether a routine with the given name exists for the specified user.
     *
     * @param name   the routine name
     * @param userId the user identifier
     * @return true if an active routine with that name exists
     */
    boolean existsByNameAndUserIdAndActiveTrue(String name, UUID userId);

    /**
     * Retrieve a paginated list of active routines for the given user, including exercise counts.
     *
     * @param userId    the user identifier
     * @param pageable  pagination parameters
     * @return a page of routine list items
     */
    @Query(value = """
            SELECT
                new dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO(
                    r.id,
                    r.name,
                    COUNT(e),
                    r.updatedAt
                )
            FROM Routine r
            LEFT JOIN r.exercises e
            WHERE r.user.id = :userId
                AND r.active = true
            GROUP BY r.id, r.name, r.updatedAt
        """, countQuery = """
                SELECT COUNT(r)
                FROM Routine r
                WHERE r.user.id = :userId
                    AND r.active = true
            """)
    Page<RoutineListItemDTO> findAllByUserIdAndActiveTrueWithExerciseCount(UUID userId, Pageable pageable);

    /**
     * Retrieve an active routine by ID with exercises and their exercises eagerly loaded.
     *
     * @param routineId the routine identifier
     * @return the routine if found and active
     */
    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Routine> findByIdAndActiveTrue(UUID routineId);

    /**
     * Retrieve an active routine by ID and user with exercises eagerly loaded.
     *
     * @param routineId the routine identifier
     * @param userId    the user identifier
     * @return the routine if found, active, and owned by the user
     */
    @EntityGraph(attributePaths = "exercises")
    Optional<Routine> findByIdAndUserIdAndActiveTrue(UUID routineId, UUID userId);
}
