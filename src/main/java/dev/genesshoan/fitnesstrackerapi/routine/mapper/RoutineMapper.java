package dev.genesshoan.fitnesstrackerapi.routine.mapper;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps routine entities to API response DTOs. */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = RoutineExerciseMapper.class)
public interface RoutineMapper {
    /**
     * Maps a routine to its detailed response DTO.
     *
     * @param routine the entity to map
     * @return the routine response
     */
    RoutineResponseDTO toRoutineResponseDTO(Routine routine);

    /**
     * Maps a routine to its list summary, including the exercise count.
     *
     * @param routine the entity to map
     * @return the routine list item
     */
    @Mapping(target = "exerciseCount", expression = "java((long) routine.getExercises().size())")
    RoutineListItemDTO toRoutineListItemDTO(Routine routine);
}
