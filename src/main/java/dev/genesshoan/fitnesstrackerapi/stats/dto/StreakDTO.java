package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.time.LocalDate;

public record StreakDTO(int currentStreak, int longestStreak, LocalDate lastActiveDay) {}
