package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementCatalogResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementFeedResult;
import dev.genesshoan.fitnesstrackerapi.gamification.application.port.in.result.AchievementSummaryResult;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementFeedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementResponseDTO;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementSummaryResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * Maps the achievements of the user to their API representations.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserAchievementMapper {

    AchievementResponseDTO toResponseDTO(AchievementCatalogResult result);

    List<AchievementResponseDTO> toResponseDTOs(List<AchievementCatalogResult> results);

    AchievementFeedResponseDTO toFeedResponseDTO(AchievementFeedResult result);

    List<AchievementFeedResponseDTO> toFeedResponseDTOs(List<AchievementFeedResult> results);

    AchievementSummaryResponseDTO toSummaryResponseDTO(AchievementSummaryResult result);
}
