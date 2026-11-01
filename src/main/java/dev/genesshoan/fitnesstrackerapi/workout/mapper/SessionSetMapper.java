package dev.genesshoan.fitnesstrackerapi.workout.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetRequest;
import dev.genesshoan.fitnesstrackerapi.workout.dto.SessionSetResponse;
import dev.genesshoan.fitnesstrackerapi.workout.dto.UnlockedAchievement;
import org.mapstruct.Builder;
import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SessionSetMapper {

    @Mapping(target = "completed", constant = "false")
    SessionSetRequest toDefaultSessionSetRequest(SessionSet sessionSet);

    @Mapping(target = "achievements", expression = "java(getAchievements(set.getId(), achievementsBySetId))")
    @Named("withAchievements")
    SessionSetResponse toResponse(SessionSet set, @Context Map<UUID, List<UnlockedAchievement>> achievementsBySetId);

    @IterableMapping(qualifiedByName = "withAchievements")
    List<SessionSetResponse> toResponses(
            List<SessionSet> sets, @Context Map<UUID, List<UnlockedAchievement>> achievementsBySetId);

    @Named("withoutAchievements")
    default SessionSetResponse toResponseWithoutAchievements(SessionSet set) {
        return toResponse(set, Map.of());
    }

    default List<UnlockedAchievement> getAchievements(
            UUID setId, @Context Map<UUID, List<UnlockedAchievement>> achievementsBySetId) {

        return achievementsBySetId.getOrDefault(setId, List.of());
    }
}
