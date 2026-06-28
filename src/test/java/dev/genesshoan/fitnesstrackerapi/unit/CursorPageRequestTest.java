package dev.genesshoan.fitnesstrackerapi.unit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.genesshoan.fitnesstrackerapi.common.utils.CursorPageRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CursorPageRequestTest {

    @Test
    @DisplayName("Should use default page size when limit is invalid")
    void validatePaginationParams_DefaultPageSize() {
        assertThat(new CursorPageRequest<>(null, null).size()).isEqualTo(20);

        assertThat(new CursorPageRequest<>(null, -1).size()).isEqualTo(20);

        assertThat(new CursorPageRequest<>(null, 10000).size()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should use provided page size when valid")
    void validatePaginationParams_ValidPageSize() {
        assertThat(new CursorPageRequest<>(null, 5).size()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create pageable with one extra item")
    void pageable_OneExtraItem() {
        assertThat(
            new CursorPageRequest<>(null, 10).pageable().getPageSize()
        ).isEqualTo(11);
    }
}
