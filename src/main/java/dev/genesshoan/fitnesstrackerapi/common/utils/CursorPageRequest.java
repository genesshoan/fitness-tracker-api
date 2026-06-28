package dev.genesshoan.fitnesstrackerapi.common.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record CursorPageRequest<C>(C cursor, Integer size) {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Override
    public Integer size() {
        return Math.min(
            size == null || size < 1 ? DEFAULT_SIZE : size,
            MAX_SIZE
        );
    }

    public Pageable pageable() {
        return PageRequest.of(0, size() + 1);
    }
}
