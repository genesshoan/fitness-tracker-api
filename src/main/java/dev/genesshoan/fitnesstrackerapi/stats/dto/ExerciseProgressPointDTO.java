package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.time.LocalDate;

public record ExerciseProgressPointDTO(LocalDate date, double weightKg, int reps, double estimatedOneRepMax) {}
