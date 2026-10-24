package dev.genesshoan.fitnesstrackerapi.exercise.mapper;

import org.springframework.beans.factory.annotation.Autowired;

import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.AssetUrlProviderPort;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseDetailDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseListItemDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.ExerciseRequestDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps catalog exercises to their API representations.
 *
 * <p>
 * The mapping methods take the exercise as their only source parameter on
 * purpose: MapStruct only delegates to a mapping method when every source
 * parameter can be satisfied from the source object. A second parameter such
 * as the resolved thumbnail URL would not be satisfiable, so MapStruct would
 * generate its own inline mapping instead of delegating here, silently
 * leaving the asset URLs null in every nested usage (routine and workout
 * responses).
 *
 * <p>
 * Resolving the URLs inside the mapper keeps the asset provider dependency
 * inside the exercise module and makes every consumer of this mapper - direct
 * or nested - resolve the URLs the same way, without having to thread any
 * extra parameter through the mapping chain.
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {ExerciseMuscleMapper.class})
public abstract class ExerciseMapper {

    @Autowired
    protected AssetUrlProviderPort assetUrlProviderPort;

    /**
     * Maps an exercise to its list item representation.
     *
     * @param exercise the exercise to map
     * @return the list item DTO, with the thumbnail URL resolved
     */
    @Mapping(
            target = "thumbnailUrl",
            expression = "java(assetUrlProviderPort.exerciseThumbnailUrl(exercise.getThumbnailObjectKey()))")
    public abstract ExerciseListItemDTO toItemDTO(Exercise exercise);

    /**
     * Maps an exercise to its detail representation.
     *
     * @param exercise the exercise to map
     * @return the detail DTO, with the GIF URL resolved
     */
    @Mapping(target = "gifUrl", expression = "java(assetUrlProviderPort.exerciseGifUrl(exercise.getMediaObjectKey()))")
    public abstract ExerciseDetailDTO toDetailDTO(Exercise exercise);

    /**
     * Maps a creation request to a new exercise entity.
     *
     * @param request the request to map
     * @return the exercise entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "exerciseMuscles", ignore = true)
    @Mapping(target = "mediaObjectKey", ignore = true)
    @Mapping(target = "thumbnailObjectKey", ignore = true)
    public abstract Exercise toEntity(ExerciseRequestDTO request);
}
