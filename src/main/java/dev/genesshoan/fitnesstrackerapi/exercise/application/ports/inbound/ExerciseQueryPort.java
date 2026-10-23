package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;

/**
 * Outbound port for exercise query operations needed by other modules.
 *
 * <p>This interface defines the read-only contract that other modules
 * (routine, workout, stats) depend on. Infrastructure adapters
 * implement this contract so that consumers do not depend on
 * a specific persistence technology.
 */
public interface ExerciseQueryPort {

    /**
     * Retrieves active exercises by their IDs.
     *
     * @param ids collection of exercise IDs
     * @return list of active exercises
     */
    List<Exercise> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    /**
     * Finds an active exercise by its ID.
     *
     * @param id the exercise UUID
     * @return optional containing the exercise if found
     */
    Optional<Exercise> findByIdAndActiveTrue(UUID id);

    /**
     * Checks if an exercise with the given id exists.
     *
     * @param id the exercise id
     * @return true if the exercise exists
     */
    boolean existsByIdAndActiveTrue(UUID id);

    /**
     * Resolves a collection of exercise IDs to a map of ID to Exercise.
     *
     * <p>Used by other modules to validate and fetch exercises
     * referenced in their domain objects.
     *
     * @param ids collection of exercise IDs
     * @return map of exercise ID to Exercise
     */
    default Map<UUID, Exercise> findActiveByIds(Collection<UUID> ids, Map<String, List<String>> errors) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        Set<UUID> uniqueIds = Set.copyOf(ids);

        List<Exercise> exercises = findAllByIdInAndActiveTrue(uniqueIds);

        Map<UUID, Exercise> exerciseMap =
                exercises.stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));

        for (UUID id : uniqueIds) {
            if (!exerciseMap.containsKey(id)) {
                errors.computeIfAbsent(id.toString(), k -> new ArrayList<>()).add("Exercise does not exist");
            }
        }

        return exerciseMap;
    }
}
