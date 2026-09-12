package dev.genesshoan.fitnesstrackerapi.stats.dto;

import java.util.List;
import java.util.UUID;

public record ExerciseProgressPointsDTO(UUID exerciseId, List<ExerciseProgressPointDTO> progress) {}
