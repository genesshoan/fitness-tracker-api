package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {ExerciseMuscleMapper.class})
public interface ExerciseMapper {
    ExerciseListItemDTO toItemDTO(Exercise exercise);

    @Mapping(target = "gifUrl", expression = "java(null)")
    ExerciseDetailDTO toDetailDTO(Exercise exercise);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "exerciseMuscles", ignore = true)
    @Mapping(target = "mediaObjectKey", ignore = true)
    Exercise toEntity(ExerciseRequestDTO request);
}
