package dev.genesshoan.fitnesstrackerapi.exercise.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExerciseFinder {

    private final ExerciseRepository exerciseRepository;

    public Map<UUID, Exercise> findActiveByIds(Collection<UUID> ids, Map<String, List<String>> errors) {

        if (ids.isEmpty()) {
            return Map.of();
        }

        Set<UUID> uniqueIds = Set.copyOf(ids);

        List<Exercise> exercises = exerciseRepository.findAllByIdInAndActiveTrue(uniqueIds);

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
