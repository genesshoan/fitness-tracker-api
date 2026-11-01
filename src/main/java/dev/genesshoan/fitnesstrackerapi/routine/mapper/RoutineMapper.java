package dev.genesshoan.fitnesstrackerapi.routine.mapper;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItem;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps routine entities to API response. */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = RoutineExerciseMapper.class)
public interface RoutineMapper {
    /**
     * Maps a routine to its detailed response.
     *
     * @param routine the entity to map
     * @return the routine response
     */
    RoutineResponse toRoutineResponse(Routine routine);

    /**
     * Maps a routine to its list summary, including the exercise count.
     *
     * @param routine the entity to map
     * @return the routine list item
     */
    @Mapping(target = "exerciseCount", expression = "java((long) routine.getExercises().size())")
    RoutineListItem toRoutineListItem(Routine routine);
}
