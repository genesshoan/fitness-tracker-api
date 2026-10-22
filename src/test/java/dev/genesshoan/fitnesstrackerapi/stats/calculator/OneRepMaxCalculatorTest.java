package dev.genesshoan.fitnesstrackerapi.stats.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OneRepMaxCalculatorTest {

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

    @Test
    @DisplayName("Should return the lifted weight when no repetitions are performed")
    void calculate_shouldReturnWeightForZeroReps() {
        assertThat(OneRepMaxCalculator.calculate(0, 100.0)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should return zero when no weight is lifted")
    void calculate_shouldReturnZeroForZeroWeight() {
        assertThat(OneRepMaxCalculator.calculate(10, 0.0)).isZero();
    }
}
