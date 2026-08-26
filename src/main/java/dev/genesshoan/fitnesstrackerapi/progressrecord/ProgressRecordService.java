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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressRecordService {

    private final ProgressRecordRepository progressRecordRepository;
    private final ProgressRecordMapper progressRecordMapper;
    private final UserRepository userRepository;

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
