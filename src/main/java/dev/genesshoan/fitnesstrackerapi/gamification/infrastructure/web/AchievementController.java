package dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.common.annotation.CurrentUserId;
import dev.genesshoan.fitnesstrackerapi.gamification.application.service.AchievementQueryService;
import dev.genesshoan.fitnesstrackerapi.gamification.domain.AchievementCategory;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementFeedResponseDTO;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementResponseDTO;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.AchievementSummaryResponseDTO;
import dev.genesshoan.fitnesstrackerapi.gamification.infrastructure.web.dto.MarkAchievementsSeenRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * HTTP adapter for the achievements of the authenticated user.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/achievements")
@Tag(name = "Achievements", description = "Endpoints for reading the achievements of the authenticated user")
public class AchievementController {

    private final AchievementQueryService achievementQueryService;
    private final UserAchievementMapper achievementMapper;

    @Operation(
            summary = "Get the achievement catalog",
            description = "Get the achievements the user can earn together with the progress they reached in each one")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Catalog retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = AchievementResponseDTO.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<AchievementResponseDTO>> getCatalog(
            @CurrentUserId UUID userId,
            @Parameter(description = "Category to filter by", example = "CONSISTENCY") @RequestParam(required = false)
                    AchievementCategory category,
            @Parameter(description = "Keep only the achievements the user unlocked")
                    @RequestParam(defaultValue = "false")
                    boolean unlockedOnly,
            @Parameter(description = "Keep only the unlocks the user has not seen yet")
                    @RequestParam(defaultValue = "false")
                    boolean unseenOnly) {

        return ResponseEntity.ok(achievementMapper.toResponseDTOs(
                achievementQueryService.getCatalog(userId, category, unlockedOnly, unseenOnly)));
    }

    @Operation(
            summary = "Get one achievement",
            description = "Get the entries of one achievement of the catalog with the state of the user. "
                    + "An achievement scoped to an exercise has one entry for each exercise the user earned it on.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Achievement retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                array = @ArraySchema(schema = @Schema(implementation = AchievementResponseDTO.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Achievement not found",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{code}")
    public ResponseEntity<List<AchievementResponseDTO>> getByCode(
            @CurrentUserId UUID userId,
            @Parameter(description = "Achievement code", required = true) @PathVariable String code) {

        return ResponseEntity.ok(achievementMapper.toResponseDTOs(achievementQueryService.getByCode(userId, code)));
    }

    @Operation(
            summary = "Get the feed of unlocked achievements",
            description = "Get the achievements the user unlocked, most recent first")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Feed retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                array =
                                        @ArraySchema(
                                                schema = @Schema(implementation = AchievementFeedResponseDTO.class)))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/feed")
    public ResponseEntity<List<AchievementFeedResponseDTO>> getFeed(
            @CurrentUserId UUID userId,
            @Parameter(description = "Keep only the unlocks the user has not seen yet")
                    @RequestParam(defaultValue = "false")
                    boolean unseenOnly,
            @Parameter(description = "Maximum number of achievements to return", example = "50")
                    @RequestParam(defaultValue = "50")
                    int limit) {

        return ResponseEntity.ok(
                achievementMapper.toFeedResponseDTOs(achievementQueryService.getFeed(userId, unseenOnly, limit)));
    }

    @Operation(
            summary = "Get the state of the collection",
            description =
                    "Get how many achievements the user unlocked, how many are unseen, and which ones are closest")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Summary retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = AchievementSummaryResponseDTO.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/summary")
    public ResponseEntity<AchievementSummaryResponseDTO> getSummary(@CurrentUserId UUID userId) {

        return ResponseEntity.ok(achievementMapper.toSummaryResponseDTO(achievementQueryService.getSummary(userId)));
    }

    @Operation(
            summary = "Mark achievements as seen",
            description = "Mark the given unlocks as seen, so the client stops showing them as new")
    @ApiResponses({
        @ApiResponse(
                responseCode = "204",
                description = "Achievements marked as seen successfully",
                content = @Content),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/seen")
    public ResponseEntity<Void> markSeen(
            @CurrentUserId UUID userId, @Valid @RequestBody MarkAchievementsSeenRequestDTO request) {

        achievementQueryService.markSeen(userId, request.achievementIds());

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
