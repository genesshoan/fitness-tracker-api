package dev.genesshoan.fitnesstrackerapi.routine.mapper;

import dev.genesshoan.fitnesstrackerapi.routine.domain.Routine;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineListItemDTO;
import dev.genesshoan.fitnesstrackerapi.routine.dto.RoutineResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true), uses = RoutineExerciseMapper.class)
public interface RoutineMapper {
    RoutineResponseDTO toRoutineResponseDTO(Routine routine);

    @Mapping(target = "exerciseCount", expression = "java((long) routine.getExercises().size())")
    RoutineListItemDTO toRoutineListItemDTO(Routine routine);
}
