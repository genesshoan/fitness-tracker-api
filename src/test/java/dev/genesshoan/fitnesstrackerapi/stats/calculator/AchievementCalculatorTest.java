package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.stats.domain.AchievementType;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolder;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementCalculatorTest {

    @Test
    @DisplayName("Should create all applicable achievements when no records exist")
    void compareData_shouldCreateAchievementsForNewRecords() {
        var achievements =
                AchievementCalculator.compareData(new ExerciseMetrics(10, 100.0, 60, 5.0), new PersonalRecordHolders());

        assertThat(achievements)
                .extracting(achievement -> achievement.type())
                .containsExactly(
                        AchievementType.NEW_MAX_WEIGHT,
                        AchievementType.NEW_ESTIMATED_1RM,
                        AchievementType.MORE_REPS_AT_WEIGHT,
                        AchievementType.NEW_MAX_DISTANCE,
                        AchievementType.NEW_MAX_DURATION);
        assertThat(achievements).allSatisfy(achievement -> {
            assertThat(achievement.previousValue()).isNull();
            assertThat(achievement.previousSetId()).isNull();
            assertThat(achievement.previousSessionId()).isNull();
        });
    }

    @Test
    @DisplayName("Should ignore metrics that are not provided")
    void compareData_shouldIgnoreNullMetrics() {
        assertThat(AchievementCalculator.compareData(
                        new ExerciseMetrics(null, null, null, null), new PersonalRecordHolders()))
                .isEmpty();
    }

    @Test
    @DisplayName("Should not create achievements when current metrics do not exceed records")
    void compareData_shouldReturnEmptyWhenRecordsAreNotExceeded() {
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        PersonalRecordHolders holders = new PersonalRecordHolders();
        holders.setMaxWeight(new PersonalRecordHolder(previousSetId, previousSessionId, 100.0));
        holders.setMax1RM(new PersonalRecordHolder(previousSetId, previousSessionId, 140.0));
        holders.setMaxDistance(new PersonalRecordHolder(previousSetId, previousSessionId, 5.0));
        holders.setMaxDuration(new PersonalRecordHolder(previousSetId, previousSessionId, 60.0));
        holders.getRepsPerWeight().put(100.0, new PersonalRecordHolder(previousSetId, previousSessionId, 10.0));

        assertThat(AchievementCalculator.compareData(new ExerciseMetrics(10, 100.0, 60, 5.0), holders))
                .isEmpty();
    }

    @Test
    @DisplayName("Should include previous record details when a record is exceeded")
    void compareData_shouldIncludePreviousRecordDetails() {
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        PersonalRecordHolders holders = new PersonalRecordHolders();
        holders.setMaxWeight(new PersonalRecordHolder(previousSetId, previousSessionId, 90.0));

        var achievements = AchievementCalculator.compareData(new ExerciseMetrics(5, 100.0, null, null), holders);

        assertThat(achievements)
                .filteredOn(achievement -> achievement.type() == AchievementType.NEW_MAX_WEIGHT)
                .singleElement()
                .satisfies(achievement -> {
                    assertThat(achievement.value()).isEqualTo(100.0);
                    assertThat(achievement.previousValue()).isEqualTo(90.0);
                    assertThat(achievement.previousSetId()).isEqualTo(previousSetId);
                    assertThat(achievement.previousSessionId()).isEqualTo(previousSessionId);
                });
    }

    @Test
    @DisplayName("Should skip weight based achievements when repetitions are missing")
    void compareData_shouldSkipWeightAchievementsWhenRepsAreMissing() {
        var achievements = AchievementCalculator.compareData(
                new ExerciseMetrics(null, 100.0, null, null), new PersonalRecordHolders());

        assertThat(achievements).isEmpty();
    }

    @Test
    @DisplayName("Should create a distance achievement when the distance record is exceeded")
    void compareData_shouldCreateDistanceAchievementWhenRecordIsExceeded() {
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        PersonalRecordHolders holders = new PersonalRecordHolders();
        holders.setMaxDistance(new PersonalRecordHolder(previousSetId, previousSessionId, 5.0));

        var achievements = AchievementCalculator.compareData(new ExerciseMetrics(null, null, null, 7.5), holders);

        assertThat(achievements)
                .filteredOn(achievement -> achievement.type() == AchievementType.NEW_MAX_DISTANCE)
                .singleElement()
                .satisfies(achievement -> {
                    assertThat(achievement.value()).isEqualTo(7.5);
                    assertThat(achievement.previousValue()).isEqualTo(5.0);
                    assertThat(achievement.previousSetId()).isEqualTo(previousSetId);
                    assertThat(achievement.previousSessionId()).isEqualTo(previousSessionId);
                });
    }

    @Test
    @DisplayName("Should create a duration achievement when the duration record is exceeded")
    void compareData_shouldCreateDurationAchievementWhenRecordIsExceeded() {
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        PersonalRecordHolders holders = new PersonalRecordHolders();
        holders.setMaxDuration(new PersonalRecordHolder(previousSetId, previousSessionId, 60.0));

        var achievements = AchievementCalculator.compareData(new ExerciseMetrics(null, null, 90, null), holders);

        assertThat(achievements)
                .filteredOn(achievement -> achievement.type() == AchievementType.NEW_MAX_DURATION)
                .singleElement()
                .satisfies(achievement -> {
                    assertThat(achievement.value()).isEqualTo(90.0);
                    assertThat(achievement.previousValue()).isEqualTo(60.0);
                    assertThat(achievement.previousSetId()).isEqualTo(previousSetId);
                    assertThat(achievement.previousSessionId()).isEqualTo(previousSessionId);
                });
    }

    @Test
    @DisplayName("Should create a repetitions achievement when more reps are performed at the same weight")
    void compareData_shouldCreateRepsAchievementWhenMoreRepsAtSameWeight() {
        UUID previousSetId = UUID.randomUUID();
        UUID previousSessionId = UUID.randomUUID();
        PersonalRecordHolders holders = new PersonalRecordHolders();
        holders.getRepsPerWeight().put(50.0, new PersonalRecordHolder(previousSetId, previousSessionId, 10.0));

        var achievements = AchievementCalculator.compareData(new ExerciseMetrics(12, 50.0, null, null), holders);

        assertThat(achievements)
                .filteredOn(achievement -> achievement.type() == AchievementType.MORE_REPS_AT_WEIGHT)
                .singleElement()
                .satisfies(achievement -> {
                    assertThat(achievement.value()).isEqualTo(12.0);
                    assertThat(achievement.previousValue()).isEqualTo(10.0);
                    assertThat(achievement.previousSetId()).isEqualTo(previousSetId);
                    assertThat(achievement.previousSessionId()).isEqualTo(previousSessionId);
                });
    }
}
