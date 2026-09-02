package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.time.LocalDate;
import java.util.List;

public class StreakCalculator {

    public record StreakResult(int currentStreak, int longestStreak) {}

    public static StreakResult calculate(List<LocalDate> activeDaysSorted, LocalDate referenceDate) {

        if (activeDaysSorted.isEmpty()) {
            return new StreakResult(0, 0);
        }

        int currentStreak = 1;
        int longestStreak = 1;

        LocalDate previousDate = null;

        for (LocalDate date : activeDaysSorted) {
            if (previousDate == null) {
                previousDate = date;
                currentStreak = 1;
                continue;
            }

            if (previousDate.plusDays(1).equals(date)) {
                currentStreak++;
            } else {
                longestStreak = Math.max(longestStreak, currentStreak);

                currentStreak = 1;
            }

            previousDate = date;
        }

        longestStreak = Math.max(longestStreak, currentStreak);

        LocalDate lastActive = activeDaysSorted.getLast();
        boolean streakStillAlive = lastActive.equals(referenceDate) || lastActive.equals(referenceDate.minusDays(1));

        currentStreak = streakStillAlive ? currentStreak : 0;

        return new StreakResult(currentStreak, longestStreak);
    }
}
