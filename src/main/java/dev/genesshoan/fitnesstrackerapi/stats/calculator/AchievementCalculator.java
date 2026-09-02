package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.stats.domain.Achievement;
import dev.genesshoan.fitnesstrackerapi.stats.domain.AchievementType;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolder;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolders;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AchievementCalculator {

    public static List<Achievement> compareData(ExerciseMetrics currentData, PersonalRecordHolders holders) {

        List<Achievement> achievements = new ArrayList<>();

        if (currentData.weightKg() != null && currentData.reps() != null) {
            achievements.addAll(compareWeightAndReps(currentData, holders));
        }

        if (currentData.distanceKm() != null) {
            compareDistance(currentData, holders).ifPresent(achievements::add);
        }

        if (currentData.durationSeconds() != null) {
            compareDuration(currentData, holders).ifPresent(achievements::add);
        }

        return achievements;
    }

    private static Optional<Achievement> compareDuration(ExerciseMetrics currentData, PersonalRecordHolders holders) {
        PersonalRecordHolder maxDuration = holders.getMaxDuration();

        if (maxDuration == null || currentData.durationSeconds() > maxDuration.getValue()) {
            return Optional.of(
                    newAchievement(AchievementType.NEW_MAX_DURATION, currentData.durationSeconds(), maxDuration));
        }

        return Optional.empty();
    }

    private static Optional<Achievement> compareDistance(ExerciseMetrics currentData, PersonalRecordHolders holders) {
        PersonalRecordHolder maxDistance = holders.getMaxDistance();

        if (maxDistance == null || currentData.distanceKm() > maxDistance.getValue()) {
            return Optional.of(newAchievement(AchievementType.NEW_MAX_DISTANCE, currentData.distanceKm(), maxDistance));
        }

        return Optional.empty();
    }

    private static List<Achievement> compareWeightAndReps(ExerciseMetrics currentData, PersonalRecordHolders holders) {
        List<Achievement> achievements = new ArrayList<>();

        PersonalRecordHolder maxWeight = holders.getMaxWeight();

        if (maxWeight == null || currentData.weightKg() > maxWeight.getValue()) {
            achievements.add(newAchievement(AchievementType.NEW_MAX_WEIGHT, currentData.weightKg(), maxWeight));
        }

        double current1RM = OneRepMaxCalculator.calculate(currentData.reps(), currentData.weightKg());

        PersonalRecordHolder max1RM = holders.getMax1RM();

        if (max1RM == null || current1RM > max1RM.getValue()) {
            achievements.add(newAchievement(AchievementType.NEW_ESTIMATED_1RM, current1RM, max1RM));
        }

        PersonalRecordHolder repsPerWeightHolder = holders.getRepsPerWeight().get(currentData.weightKg());

        if (repsPerWeightHolder == null || currentData.reps() > repsPerWeightHolder.getValue()) {
            achievements.add(
                    newAchievement(AchievementType.MORE_REPS_AT_WEIGHT, currentData.reps(), repsPerWeightHolder));
        }

        return achievements;
    }

    private static Achievement newAchievement(AchievementType type, double value, PersonalRecordHolder holder) {
        Double previousValue = null;
        UUID previousSetId = null;
        UUID previousSessionId = null;

        if (holder != null) {
            previousValue = holder.getValue();
            previousSetId = holder.getSetId();
            previousSessionId = holder.getSessionId();
        }

        return new Achievement(type, value, previousValue, previousSetId, previousSessionId);
    }
}
