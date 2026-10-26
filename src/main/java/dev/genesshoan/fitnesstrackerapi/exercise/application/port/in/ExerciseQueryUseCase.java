package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;

/**
 * Read-only view of the exercise catalog that other modules depend on.
 *
 * <p>Implemented by the exercise module and consumed by the routine, workout and
 * stats modules, so those modules never depend on the exercise repository.
 *
 * <p>Every method is limited to active exercises: soft-deleted exercises are
 * never visible through this contract.
 */
public interface ExerciseQueryUseCase {

    /**
     * Checks whether an active exercise exists with the given id.
     *
     * <p>Use this when only the existence of the exercise matters.
     *
     * @param id the exercise id
     * @return {@code true} if an active exercise exists with that id
     */
    boolean existsById(UUID id);

    /**
     * Finds an active exercise by its id.
     *
     * <p>The returned entity is meant for callers that persist a relation to the
     * exercise, such as a routine assignment or a session exercise. Callers that
     * only need to know whether the exercise exists should use
     * {@link #existsById(UUID)}.
     *
     * @param id the exercise id
     * @return the matching exercise, or empty if it does not exist or is inactive
     */
    Optional<Exercise> findById(UUID id);

    /**
     * Finds the active exercises with the given ids, keyed by exercise id.
     *
     * <p>Ids without a matching active exercise are simply absent from the
     * returned map, so callers can detect them by comparing the map keys with
     * the requested ids.
     *
     * @param ids the ids of the exercises to find
     * @return the matching active exercises, keyed by exercise id
     */
    Map<UUID, Exercise> findByIds(Collection<UUID> ids);
}
