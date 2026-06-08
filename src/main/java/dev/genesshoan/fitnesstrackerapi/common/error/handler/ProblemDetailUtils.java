package dev.genesshoan.fitnesstrackerapi.common.error.handler;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility helpers for creating ProblemDetail responses and grouping errors.
 */
public class ProblemDetailUtils {
    /**
     * Group a collection of items into a Map of lists by a key mapper and
     * transform values with a value mapper. Commonly used to group validation
     * errors by field name.
     */
    public static <T, K, V> Map<K, List<V>> groupErrors(
            Collection<T> items,
            Function<T, K> keyMapper,
            Function<T, V> valueMapper) {

        return items.stream()
                .collect(Collectors.groupingBy(
                        keyMapper,
                        Collectors.mapping(valueMapper, Collectors.toList())));
    }

    /**
     * Build a ProblemDetail with common properties populated (status, title,
     * detail, errors, timestamp, path).
     *
     * @param status HTTP status to use
     * @param title short title for the problem
     * @param detail human-readable detail message
     * @param errors optional map of field errors
     * @param request current HTTP request (used to populate path)
     */
    public static ProblemDetail errorResponse(
            HttpStatus status,
            String title,
            String detail,
            @Nullable Map<String, ?> errors,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(title);
        pd.setDetail(detail);

        if (errors != null && !errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }

        pd.setProperty("timestamp", Instant.now());
        pd.setProperty("path", request.getRequestURI());

        return pd;
    }
}
