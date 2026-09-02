package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SessionSetMapper {

    SessionSetResponseDTO toSessionSetResponseDTO(SessionSet sessionSet);

    @Mapping(target = "completed", constant = "false")
    SessionSetRequestDTO toDefaultSessionSetRequestDTO(SessionSet sessionSet);
}
