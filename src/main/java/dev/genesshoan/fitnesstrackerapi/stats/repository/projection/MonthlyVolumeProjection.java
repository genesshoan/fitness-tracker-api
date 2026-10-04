package dev.genesshoan.fitnesstrackerapi.stats.repository.projection;

/**
 * JDBC projection for volume aggregated by a user's local calendar month.
 *
 * @param month calendar month in {@code yyyy-MM} format
 * @param volumeKg total kilogram-repetitions
 */
public record MonthlyVolumeProjection(String month, double volumeKg) {}
