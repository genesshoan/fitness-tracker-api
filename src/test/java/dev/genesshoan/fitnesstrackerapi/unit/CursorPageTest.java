package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPage;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CursorPageTest {

    @Test
    @DisplayName("Should calculate next cursor correctly")
    void calculateNextCursor_ShouldReturnCorrectValue() {
        // Given
        var id1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var id2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var id3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

        var items = List.of(id1, id2, id3);

        // When
        var cursorPage = CursorPage.of(
            items,
            2,
            Function.identity(),
            Function.identity()
        );

        // Then
        assertThat(cursorPage.page()).hasSize(2);
        assertThat(cursorPage.hasNext()).isTrue();
        assertThat(cursorPage.nextCursor()).hasValue(id2);
    }

    @Test
    @DisplayName("Should handle empty list")
    void cursorPagination_EmptyList() {
        // When
        var cursorPage = CursorPage.of(
            List.of(),
            2,
            Function.identity(),
            Function.identity()
        );

        // Then
        assertThat(cursorPage.page()).isEmpty();
        assertThat(cursorPage.hasNext()).isFalse();
        assertThat(cursorPage.nextCursor()).isEmpty();
    }

    @Test
    @DisplayName("Should handle less items than page size")
    void cursorPagination_LessItemsThanPageSize() {
        // When
        var cursorPage = CursorPage.of(
            List.of(UUID.randomUUID()),
            20,
            Function.identity(),
            Function.identity()
        );

        // Then
        assertThat(cursorPage.page()).hasSize(1);
        assertThat(cursorPage.hasNext()).isFalse();
        assertThat(cursorPage.nextCursor()).isEmpty();
    }

    @Test
    @DisplayName("Should handle exact page size")
    void cursorPagination_ExactPageSize() {
        // When
        var cursorPage = CursorPage.of(
            List.of(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("22222222-2222-2222-2222-222222222222")
            ),
            2,
            Function.identity(),
            Function.identity()
        );

        // Then
        assertThat(cursorPage.page()).hasSize(2);
        assertThat(cursorPage.hasNext()).isFalse();
        assertThat(cursorPage.nextCursor()).isEmpty();
    }

    @Test
    @DisplayName("Should map page items")
    void cursorPagination_ShouldMapItems() {
        // Given
        var items = List.of(1, 2, 3);

        // When
        var cursorPage = CursorPage.of(
            items,
            2,
            Function.identity(),
            i -> "Item " + i
        );

        // Then
        assertThat(cursorPage.page()).containsExactly("Item 1", "Item 2");
    }
}
