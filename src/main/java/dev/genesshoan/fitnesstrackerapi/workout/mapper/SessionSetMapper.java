package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequestDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponseDTO;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievementDTO;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SessionSetMapper {

    @Mapping(target = "completed", constant = "false")
    SessionSetRequestDTO toDefaultSessionSetRequestDTO(SessionSet sessionSet);

    @Mapping(target = "achievements", expression = "java(getAchievements(set.getId(), achievementsBySetId))")
    @Named("withAchievements")
    SessionSetResponseDTO toResponseDTO(
            SessionSet set, @Context Map<UUID, List<UnlockedAchievementDTO>> achievementsBySetId);

    @IterableMapping(qualifiedByName = "withAchievements")
    List<SessionSetResponseDTO> toResponseDTOs(
            List<SessionSet> sets, @Context Map<UUID, List<UnlockedAchievementDTO>> achievementsBySetId);

    @Named("withoutAchievements")
    default SessionSetResponseDTO toResponseWithoutAchievements(SessionSet set) {
        return toResponseDTO(set, Map.of());
    }

    default List<UnlockedAchievementDTO> getAchievements(
            UUID setId, @Context Map<UUID, List<UnlockedAchievementDTO>> achievementsBySetId) {

        return achievementsBySetId.getOrDefault(setId, List.of());
    }
}
