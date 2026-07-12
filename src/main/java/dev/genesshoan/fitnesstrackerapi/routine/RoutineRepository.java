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

@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    boolean existsByNameAndUserIdAndActiveTrue(String name, UUID userId);

    @Query(
            value =
                    """
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
        """,
            countQuery =
                    """
            SELECT COUNT(r)
            FROM Routine r
            WHERE r.user.id = :userId
                AND r.active = true
        """)
    Page<RoutineListItemDTO> findAllByUserIdAndActiveTrueWithExerciseCount(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"exercises", "exercises.exercise"})
    Optional<Routine> findByIdAndActiveTrue(UUID routineId);
}
