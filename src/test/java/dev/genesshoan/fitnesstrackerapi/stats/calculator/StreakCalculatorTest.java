package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StreakCalculatorTest {

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

    @Test
    @DisplayName("Should keep the current streak alive when the last active day is yesterday")
    void calculate_shouldKeepCurrentStreakWhenLastActiveDayIsYesterday() {
        var result = StreakCalculator.calculate(
                List.of(LocalDate.of(2026, 9, 11), LocalDate.of(2026, 9, 12)), LocalDate.of(2026, 9, 13));

        assertThat(result.currentStreak()).isEqualTo(2);
        assertThat(result.longestStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count a single active day as a streak of one")
    void calculate_shouldCountSingleActiveDayAsStreakOfOne() {
        var result = StreakCalculator.calculate(List.of(LocalDate.of(2026, 9, 13)), LocalDate.of(2026, 9, 13));

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.longestStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should keep the longest streak when the current streak is broken")
    void calculate_shouldKeepLongestStreakWhenCurrentStreakIsBroken() {
        var result = StreakCalculator.calculate(
                List.of(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 2),
                        LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 10)),
                LocalDate.of(2026, 9, 10));

        assertThat(result.currentStreak()).isEqualTo(1);
        assertThat(result.longestStreak()).isEqualTo(3);
    }
}
