package dev.genesshoan.fitnesstrackerapi.stats.mapper;

/** Maps achievement domain values to API DTOs. */
import java.util.List;

import dev.genesshoan.fitnesstrackerapi.stats.domain.Achievement;
import dev.genesshoan.fitnesstrackerapi.stats.dto.AchievementDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AchievementMapper {

    AchievementDTO toDto(Achievement achievement);

    List<AchievementDTO> toDtos(List<Achievement> achievements);
}
