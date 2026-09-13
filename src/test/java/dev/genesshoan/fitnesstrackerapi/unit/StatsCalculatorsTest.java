package dev.genesshoan.fitnesstrackerapi.unit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.common.domain.ExerciseMetrics;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.AchievementCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.OneRepMaxCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.StreakCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.calculator.VolumeCalculator;
import dev.genesshoan.fitnesstrackerapi.stats.domain.AchievementType;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolder;
import dev.genesshoan.fitnesstrackerapi.stats.domain.PersonalRecordHolders;
import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatsCalculatorsTest {

    @Nested
    @DisplayName("StreakCalculator")
    class StreakCalculatorTests {

        @Test
        @DisplayName("Should return zeroes when no active days exist")
        void calculate_shouldReturnZeroesForEmptyDays() {
            assertThat(StreakCalculator.calculate(List.of(), LocalDate.of(2026, 9, 13)))
                    .isEqualTo(new StreakCalculator.StreakResult(0, 0));
        }

        @Test
        @DisplayName("Should calculate current and longest consecutive streaks")
        void calculate_shouldReturnCurrentAndLongestStreaks() {
            var result = StreakCalculator.calculate(
                    List.of(
                            LocalDate.of(2026, 9, 1),
                            LocalDate.of(2026, 9, 2),
                            LocalDate.of(2026, 9, 5),
                            LocalDate.of(2026, 9, 6),
                            LocalDate.of(2026, 9, 7)),
                    LocalDate.of(2026, 9, 7));

            assertThat(result.currentStreak()).isEqualTo(3);
            assertThat(result.longestStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should clear current streak when the last active day is too old")
        void calculate_shouldReturnZeroCurrentStreakWhenStreakIsNotAlive() {
            var result = StreakCalculator.calculate(
                    List.of(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2)), LocalDate.of(2026, 9, 5));

            assertThat(result.currentStreak()).isZero();
            assertThat(result.longestStreak()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("VolumeCalculator")
    class VolumeCalculatorTests {

        @Test
        @DisplayName("Should sum weight multiplied by repetitions")
        void calculate_shouldSumSetVolumes() {
            assertThat(VolumeCalculator.calculate(
                            List.of(new VolumeSetProjection(100.0, 5), new VolumeSetProjection(60.0, 10))))
                    .isEqualTo(1100.0);
        }

        @Test
        @DisplayName("Should return zero for no sets")
        void calculate_shouldReturnZeroForEmptySets() {
            assertThat(VolumeCalculator.calculate(List.of())).isZero();
        }
    }

    @Nested
    @DisplayName("OneRepMaxCalculator")
    class OneRepMaxCalculatorTests {

        @Test
        @DisplayName("Should calculate estimated one rep max using the Epley formula")
        void calculate_shouldReturnEstimatedOneRepMax() {
            assertThat(OneRepMaxCalculator.calculate(5, 100.0)).isEqualTo(116.66666666666667);
        }

        @Test
        @DisplayName("Should return the lifted weight for one repetition")
        void calculate_shouldReturnWeightForOneRep() {
            assertThat(OneRepMaxCalculator.calculate(1, 80.0)).isEqualTo(82.66666666666667);
        }
    }

    @Nested
    @DisplayName("AchievementCalculator")
    class AchievementCalculatorTests {

        @Test
        @DisplayName("Should create all applicable achievements when no records exist")
        void compareData_shouldCreateAchievementsForNewRecords() {
            var achievements = AchievementCalculator.compareData(
                    new ExerciseMetrics(10, 100.0, 60, 5.0), new PersonalRecordHolders());

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
    }
}
