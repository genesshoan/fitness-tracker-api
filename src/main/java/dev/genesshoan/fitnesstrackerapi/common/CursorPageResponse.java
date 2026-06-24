package dev.genesshoan.fitnesstrackerapi.common;

import java.util.List;
import java.util.UUID;

public record CursorPageResponse<T>(
    List<T> data,
    UUID nextCursor,
    boolean hasNext
) {}
