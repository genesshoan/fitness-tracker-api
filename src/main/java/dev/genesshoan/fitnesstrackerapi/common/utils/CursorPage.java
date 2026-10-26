package dev.genesshoan.fitnesstrackerapi.common.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A page of results with a cursor for pagination")
public record CursorPage<T, C>(List<T> page, Optional<C> nextCursor, boolean hasNext) {
    public static <T, C, D> CursorPage<D, C> of(
            List<T> data, int pageSize, Function<T, C> cursorMapper, Function<T, D> resultMapper) {
        boolean hasNext = data.size() > pageSize;

        List<T> rawPage = hasNext ? data.subList(0, pageSize) : data;

        Optional<C> nextCursor = hasNext ? Optional.of(cursorMapper.apply(rawPage.getLast())) : Optional.empty();

        List<D> mappedPage = rawPage.stream().map(resultMapper).toList();

        return new CursorPage<>(mappedPage, nextCursor, hasNext);
    }

    /**
     * Maps the page content to another type, keeping the cursor and the
     * pagination state untouched.
     *
     * @param mapper the function applied to every item of the page
     * @param <D>    the type of the mapped items
     * @return the page with the mapped content
     */
    public <D> CursorPage<D, C> map(Function<T, D> mapper) {
        return new CursorPage<>(page.stream().map(mapper).toList(), nextCursor, hasNext);
    }
}
