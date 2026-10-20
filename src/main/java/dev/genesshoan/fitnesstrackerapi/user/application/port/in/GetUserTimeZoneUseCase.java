package dev.genesshoan.fitnesstrackerapi.user.application.port.in;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for retrieving a user's time zone.
 */
public interface GetUserTimeZoneUseCase {

    /**
     * Retrieves the time zone associated with the specified user.
     *
     * @param userId the ID of the user
     * @return an {@link Optional} containing the user's time zone if available,
     *         or an empty {@link Optional} if the user has no time zone configured
     */
    Optional<ZoneId> getUserTimezoneById(UUID userId);
}
