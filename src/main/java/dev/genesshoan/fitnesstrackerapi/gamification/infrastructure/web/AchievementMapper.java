package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementCatalogResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementFeedResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementFeedResponse;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementResponse;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementSummaryResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * Maps the achievements of the user to their API representations.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface AchievementMapper {

    AchievementResponse toResponse(AchievementCatalogResult result);

    List<AchievementResponse> toResponses(List<AchievementCatalogResult> results);

    AchievementFeedResponse toFeedResponse(AchievementFeedResult result);

    List<AchievementFeedResponse> toFeedResponses(List<AchievementFeedResult> results);

    AchievementSummaryResponse toSummaryResponse(AchievementSummaryResult result);
}
