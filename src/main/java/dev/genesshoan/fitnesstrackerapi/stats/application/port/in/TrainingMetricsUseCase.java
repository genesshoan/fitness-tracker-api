package dev.genesshoan.fitnesstrackerapi.stats.application.port.in;

import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.stats.application.port.in.result.TrainingSnapshot;

/**
 * Reads the training aggregates of a user.
 *
 * <p>This is the read side of the statistics module that other modules consume.
 * The aggregates are computed when they are asked for, not maintained as
 * denormalized counters, so they cannot drift from the training history.</p>
 */
public interface TrainingMetricsUseCase {

    /**
     * Builds the training snapshot of a user.
     *
     * @param userId the user the aggregates belong to
     * @return the training aggregates of the user
     * @throws dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException
     *         if the user does not exist
     */
    TrainingSnapshot getSnapshot(UUID userId);
}
