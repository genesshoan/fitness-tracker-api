package dev.genesshoan.fitnesstrackerapi.unit;

import dev.genesshoan.fitnesstrackerapi.exercise.ExerciseNameHighlighter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseNameHighlighterTest {

    @Test
    void highlight_shouldPreserveCaseAndEscapeNameMarkup() {
        assertThat(ExerciseNameHighlighter.highlight("Fish & Chips <deluxe>", "chips"))
                .isEqualTo("Fish &amp; <b>Chips</b> &lt;deluxe&gt;");
    }

    @Test
    void highlight_shouldWrapEveryCaseInsensitiveOccurrence() {
        assertThat(ExerciseNameHighlighter.highlight("Press Press", "press")).isEqualTo("<b>Press</b> <b>Press</b>");
    }
}
