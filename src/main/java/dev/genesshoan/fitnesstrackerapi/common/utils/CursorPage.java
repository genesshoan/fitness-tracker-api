package dev.genesshoan.fitnesstrackerapi.common.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Schema(description = "A page of results with a cursor for pagination")
public record CursorPage<T, C>(
    List<T> page,
    Optional<C> nextCursor,
    boolean hasNext
) {
    public static <T, C, D> CursorPage<D, C> of(
        List<T> data,
        int pageSize,
        Function<T, C> cursorMapper,
        Function<T, D> resultMapper
    ) {
        boolean hasNext = data.size() > pageSize;

        List<T> rawPage = hasNext ? data.subList(0, pageSize) : data;

        Optional<C> nextCursor = hasNext
            ? Optional.of(cursorMapper.apply(rawPage.getLast()))
            : Optional.empty();

        List<D> mappedPage = rawPage.stream().map(resultMapper).toList();

        return new CursorPage<>(mappedPage, nextCursor, hasNext);
    }
}
