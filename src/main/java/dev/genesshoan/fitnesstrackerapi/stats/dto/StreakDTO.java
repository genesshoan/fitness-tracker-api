package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User workout streak statistics")
public record StreakDTO(
        @Schema(description = "Number of consecutive active days ending today or yesterday", example = "5")
        int currentStreak,

        @Schema(description = "Longest consecutive active-day streak", example = "12")
        int longestStreak,

        @Schema(description = "Most recent active day", example = "2026-09-12", nullable = true)
        LocalDate lastActiveDay) {}
