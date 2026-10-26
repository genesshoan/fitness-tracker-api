package dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web;

import org.springframework.beans.factory.annotation.Autowired;

import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.command.ExerciseCommand;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseDetailResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseListItemResult;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.ExerciseSuggestion;
import dev.genesshoan.fitnesstrackerapi.exercise.domain.Exercise;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.storage.CatalogAssetUrlProvider;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.ExerciseDetail;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.ExerciseListItem;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.ExerciseRequest;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.web.dto.ExerciseSearchResult;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps catalog exercises to their API representations.
 *
 * <p>
 * The mapping from an exercise to a list item takes the exercise as its only
 * source parameter on purpose: MapStruct only delegates to a mapping method
 * when every source parameter can be satisfied from the source object. A second
 * parameter such as the resolved thumbnail URL would not be satisfiable, so
 * MapStruct would generate its own inline mapping instead of delegating here,
 * silently leaving the asset URLs null in every nested usage (routine and
 * workout responses).
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
        uses = {MuscleMapper.class})
public abstract class ExerciseMapper {

    @Autowired
    protected CatalogAssetUrlProvider assetUrlProvider;

    /**
     * Maps an exercise to its list item representation.
     *
     * <p>Used by the other modules to render the exercise nested in their own
     * responses, such as the routine and the workout session responses.
     *
     * @param exercise the exercise to map
     * @return the list item DTO, with the thumbnail URL resolved
     */
    @Mapping(
            target = "thumbnailUrl",
            expression = "java(assetUrlProvider.exerciseThumbnailUrl(exercise.getThumbnailObjectKey()))")
    public abstract ExerciseListItem toItemDTO(Exercise exercise);

    /**
     * Maps an exercise list item result to its API representation.
     *
     * @param result the result to map
     * @return the list item DTO
     */
    public abstract ExerciseListItem toItemDTO(ExerciseListItemResult result);

    /**
     * Maps an exercise detail result to its API representation.
     *
     * @param result the result to map
     * @return the detail DTO
     */
    public abstract ExerciseDetail toDetailDTO(ExerciseDetailResult result);

    /**
     * Maps a search suggestion to its API representation.
     *
     * @param suggestion the suggestion to map
     * @return the search result DTO
     */
    public abstract ExerciseSearchResult toSearchResultDTO(ExerciseSuggestion suggestion);

    /**
     * Maps an exercise write request to its command.
     *
     * @param request the request to map
     * @return the command with the submitted exercise data
     */
    public abstract ExerciseCommand toCommand(ExerciseRequest request);
}
