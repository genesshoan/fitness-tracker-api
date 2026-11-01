package dev.genesshoan.fitnesstrackerapi.progressrecord.mapper;

import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordRequest;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProgressRecordMapper {

    ProgressRecordResponse toResponse(ProgressRecord progressRecord);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    ProgressRecord toEntity(ProgressRecordRequest request);
}
