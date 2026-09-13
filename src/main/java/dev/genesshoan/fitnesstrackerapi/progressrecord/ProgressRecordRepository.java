package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for progress record persistence.
 *
 * <p>Enforces unique constraint on (user_id, recorded_at) at the database level.
 * Duplicate records for the same user on the same date will throw a
 * {@link org.springframework.dao.DataIntegrityViolationException} on save.
 */
@Repository
public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, UUID> {

    /**
     * Finds all progress records for a user within a date range, ordered by date ascending.
     *
     * @param userId   the user ID
     * @param from     the start date (inclusive)
     * @param to       the end date (inclusive)
     * @param pageable pagination parameters
     * @return a {@link Page} of progress records ordered by date ascending
     */
    Page<ProgressRecord> findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Checks whether a progress record exists for a specific user.
     *
     * @param progressRecordId the progress record ID
     * @param userId           the user ID
     * @return true if the progress record exists and belongs to the user
     */
    boolean existsByIdAndUserId(UUID progressRecordId, UUID userId);
}
