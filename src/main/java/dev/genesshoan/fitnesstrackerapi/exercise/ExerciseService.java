package dev.genesshoan.fitnesstrackerapi.exercise;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Category;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Difficulty;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.ExerciseMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final ExerciseMapper exerciseMapper;

    public CursorPage<ExerciseListItemDTO, UUID> getAllExercises(
        CursorPageRequest<UUID> request,
        Category category,
        Difficulty difficulty,
        List<String> muscleSlugs
    ) {
        List<Exercise> exercises = exerciseRepository.findByFilters(
            request.cursor(),
            category,
            difficulty,
            muscleSlugs,
            request.pageable()
        );

        log.info("Found {} exercises", exercises.size());

        return CursorPage.of(
            exercises,
            request.size(),
            Exercise::getId,
            exerciseMapper::toItemDTO
        );
    }

    public ExerciseDetailDTO getExerciseBySlug(String slug) {
        var exercise = exerciseRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Exercise with slug {} not found", slug);
            return new ResourceNotFoundException(
                "Exercise with slug " + slug + " not found"
            );
        });

        log.info("Found exercise with slug {}", slug);

        return exerciseMapper.toDetailDTO(exercise);
    }
}
