package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordResponseDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.mapper.ProgressRecordMapper;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing user progress records (weight and body fat measurements).
 *
 * <p>Provides operations to create, retrieve, and delete progress records.
 * Each record represents a snapshot of the user's body metrics at a specific date.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Progress records are immutable after creation (weight and date cannot be changed)</li>
 *   <li>Each user can have at most one progress record per date (unique constraint)</li>
 *   <li>Records can be filtered by date range for charting/trending views</li>
 *   <li>All operations require authentication and validate user ownership</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressRecordService {

    private final ProgressRecordRepository progressRecordRepository;
    private final ProgressRecordMapper progressRecordMapper;
    private final UserRepository userRepository;

    /**
     * Retrieves progress records for a user within a specified date range.
     *
     * <p>Returns records ordered by date ascending (oldest first). The date range
     * is inclusive of both start and end dates. Throws an exception if the start
     * date is after the end date.
     *
     * @param userId   the ID of the user whose records to retrieve
     * @param from     the start date of the range (inclusive)
     * @param to       the end date of the range (inclusive)
     * @param pageable pagination parameters (page number, size, sort)
     * @return a {@link Page} of {@link ProgressRecordResponseDTO} objects
     * @throws BadRequestException if {@code from} is after {@code to}
     */
    public Page<ProgressRecordResponseDTO> getProgressRecordsInDateRange(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from.isAfter(to)) {
            throw new BadRequestException("The start date cannot be after the end date");
        }

        log.debug("Fetching progress records for user {} between {} and {}", userId, from, to);

        return progressRecordRepository
                .findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to, pageable)
                .map(progressRecordMapper::toResponseDTO);
    }

    /**
     * Creates a new progress record for the specified user on a given date.
     *
     * <p>Validates that no progress record already exists for the user on the given date.
     * If a record already exists for that date, throws a {@link ResourceAlreadyExistsException}.
     * The recordedAt and weightKg fields are set as immutable (cannot be updated after creation).
     *
     * @param userId     the ID of the user creating the record
     * @param requestDTO the progress record data (date, weight, optional body fat percentage)
     * @return a {@link ProgressRecordResponseDTO} representing the created record
     * @throws ResourceAlreadyExistsException if a progress record already exists for the user on the given date
     */
    @Transactional
    public ProgressRecordResponseDTO createProgressRecord(UUID userId, ProgressRecordRequestDTO requestDTO) {

        log.debug("Creating progress record for user {} on {}", userId, requestDTO.recordedAt());

        ProgressRecord progressRecord = progressRecordMapper.toEntity(requestDTO);
        progressRecord.setUser(userRepository.getReferenceById(userId));

        try {
            progressRecordRepository.save(progressRecord);
        } catch (DataIntegrityViolationException e) {
            log.warn(
                    "Progress record creation failed due to duplicate record for user {} on {}",
                    userId,
                    requestDTO.recordedAt(),
                    e);

            throw new ResourceAlreadyExistsException("A progress record already exists for this date");
        }

        log.debug("Progress record created successfully for user {} on {}", userId, requestDTO.recordedAt());

        return progressRecordMapper.toResponseDTO(progressRecord);
    }

    /**
     * Deletes a progress record belonging to the specified user.
     *
     * <p>Verifies that the progress record belongs to the user before deletion.
     * If the record does not exist or belongs to another user, throws a {@link ResourceNotFoundException}.
     * This prevents users from deleting each other's records.
     *
     * @param progressRecordId the ID of the progress record to delete
     * @param userId           the ID of the user who owns the record
     * @throws ResourceNotFoundException if the progress record does not exist or does not belong to the user
     */
    @Transactional
    public void deleteProgressRecord(UUID progressRecordId, UUID userId) {

        log.debug("Deleting progress record {} for user {}", progressRecordId, userId);

        if (!progressRecordRepository.existsByIdAndUserId(progressRecordId, userId)) {

            log.info("Progress record {} not found for user {}", progressRecordId, userId);

            throw new ResourceNotFoundException("Progress record not found");
        }

        progressRecordRepository.deleteById(progressRecordId);

        log.debug("Progress record {} deleted successfully for user {}", progressRecordId, userId);
    }
}
