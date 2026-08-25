package dev.genesshoan.fitnesstrackerapi.progressrecord.mapper;

import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProgressRecordMapper {

    ProgressRecordResponseDTO toResponseDTO(ProgressRecord progressRecord);

    @Mapping(target = "user", ignore = true)
    ProgressRecord toEntity(ProgressRecordRequestDTO request);
}
