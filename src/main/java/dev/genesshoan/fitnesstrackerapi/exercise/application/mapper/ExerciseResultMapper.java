package dev.genesshoan.fitnesstrackerapi.exercise.application.mapper;

import org.springframework.beans.factory.annotation.Autowired;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseDetailResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseListItemResult;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.storage.CatalogAssetUrlProvider;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps the exercise catalog to the values the application layer exposes.
 *
 * <p>The asset URLs are resolved here so every consumer of the exercise results,
 * the exercise controller included, gets the same URLs without having to know
 * how the catalog assets are stored.
 */
@Mapper(
        componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        uses = {MuscleResultMapper.class})
public abstract class ExerciseResultMapper {

    @Autowired
    protected CatalogAssetUrlProvider assetUrlProvider;

    /**
     * Maps an exercise to its list item result.
     *
     * @param exercise the exercise to map
     * @return the list item result, with the thumbnail URL resolved
     */
    @Mapping(
            target = "thumbnailUrl",
            expression = "java(assetUrlProvider.exerciseThumbnailUrl(exercise.getThumbnailObjectKey()))")
    public abstract ExerciseListItemResult toListItem(Exercise exercise);

    /**
     * Maps an exercise to its detail result.
     *
     * @param exercise the exercise to map
     * @return the detail result, with the media URL resolved
     */
    @Mapping(target = "gifUrl", expression = "java(assetUrlProvider.exerciseGifUrl(exercise.getMediaObjectKey()))")
    public abstract ExerciseDetailResult toDetail(Exercise exercise);

    /**
     * Maps a command to a new exercise entity.
     *
     * <p>The generated fields and the muscle associations are not part of the
     * command: they are resolved by the service.
     *
     * @param command the command to map
     * @return the exercise entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "exerciseMuscles", ignore = true)
    @Mapping(target = "mediaObjectKey", ignore = true)
    @Mapping(target = "thumbnailObjectKey", ignore = true)
    public abstract Exercise toEntity(ExerciseCommand command);
}
