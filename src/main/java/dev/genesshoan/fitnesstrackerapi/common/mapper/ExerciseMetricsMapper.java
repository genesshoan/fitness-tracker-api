package dev.genesshoan.fitnesstrackerapi.common.mapper;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExerciseMetricsMapper {

    @Mapping(target = "setNumber", constant = "1")
    @Mapping(target = "completed", constant = "false")
    SessionSetRequestDTO toDefaultSessionSetRequestDTO(ExerciseMetrics exerciseMetrics);
}
