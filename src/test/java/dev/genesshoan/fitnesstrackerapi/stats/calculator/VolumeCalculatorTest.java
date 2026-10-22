package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import java.util.List;

import dev.genesshoan.fitnesstrackerapi.stats.repository.projection.VolumeSetProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeCalculatorTest {

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

    @Test
    @DisplayName("Should multiply a fractional weight by its repetitions")
    void calculate_shouldMultiplyFractionalWeightByReps() {
        assertThat(VolumeCalculator.calculate(List.of(new VolumeSetProjection(12.5, 8))))
                .isEqualTo(100.0);
    }
}
