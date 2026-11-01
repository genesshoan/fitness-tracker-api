package dev.genesshoan.fitnesstrackerapi.common.mapper;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExerciseMetricsMapper {

    @Mapping(target = "setNumber", constant = "1")
    @Mapping(target = "completed", constant = "false")
    SessionSetRequest toDefaultSessionSetRequest(ExerciseMetrics exerciseMetrics);

    ExerciseMetrics toExerciseMetrics(SessionSet sessionSet);
}
