package dev.genesshoan.fitnesstrackerapi.unit;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.BadRequestException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceAlreadyExistsException;
import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecordRepository;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecordService;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordRequestDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.dto.ProgressRecordResponseDTO;
import dev.genesshoan.fitnesstrackerapi.progressrecord.mapper.ProgressRecordMapper;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ProgressRecordBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.UserRepository;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProgressRecordServiceTest {

    private static final Faker FAKER = new Faker();
    private static UUID PROGRESS_RECORD_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID USER_ID = UUID.fromString("01932f4a-1234-7000-8000-123456789abc");
    private static final LocalDate RECORDED_AT = LocalDate.of(2026, 8, 25);
    private static final double WEIGHT_KG = 75.5;
    private static final double BODY_FAT_PERCENTAGE = 20.0;

    @Mock
    private ProgressRecordRepository progressRecordRepository;

    @Mock
    private ProgressRecordMapper progressRecordMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProgressRecordService progressRecordService;

    private ProgressRecord progressRecord;
    private User user;
    private ProgressRecordRequestDTO requestDTO;
    private ProgressRecordResponseDTO responseDTO;

    @BeforeEach
    void setup() {
        user = UserBuilder.aUser(FAKER).withId(USER_ID).build();

        progressRecord = ProgressRecordBuilder.aProgressRecord(FAKER)
                .forUser(user)
                .withRecordedAt(RECORDED_AT)
                .withWeightKg(WEIGHT_KG)
                .withBodyFatPercentage(BODY_FAT_PERCENTAGE)
                .build();

        requestDTO = new ProgressRecordRequestDTO(RECORDED_AT, WEIGHT_KG, BODY_FAT_PERCENTAGE);
    }

    @Nested
    @DisplayName("createProgressRecord")
    class CreateProgressRecord {

        @Test
        @DisplayName("Should create a progress record successfully")
        void shouldCreateAProgressRecordSuccessfully() {
            when(progressRecordMapper.toEntity(requestDTO)).thenReturn(progressRecord);

            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            when(progressRecordMapper.toResponseDTO(progressRecord)).thenReturn(responseDTO);

            ProgressRecordResponseDTO result = progressRecordService.createProgressRecord(USER_ID, requestDTO);

            assertThat(result).isSameAs(responseDTO);
            assertThat(progressRecord.getUser()).isSameAs(user);

            verify(progressRecordMapper).toEntity(requestDTO);
            verify(userRepository).getReferenceById(USER_ID);
            verify(progressRecordRepository).save(progressRecord);
            verify(progressRecordMapper).toResponseDTO(progressRecord);
        }

        @Test
        @DisplayName("Should throw when progress record already exists for date")
        void shouldThrowWhenProgressRecordAlreadyExists() {
            when(progressRecordMapper.toEntity(requestDTO)).thenReturn(progressRecord);
            when(userRepository.getReferenceById(USER_ID)).thenReturn(user);

            when(progressRecordRepository.save(progressRecord))
                    .thenThrow(new DataIntegrityViolationException("duplicate key"));

            assertThatThrownBy(() -> progressRecordService.createProgressRecord(USER_ID, requestDTO))
                    .isInstanceOf(ResourceAlreadyExistsException.class)
                    .hasMessage("A progress record already exists for this date");

            verify(progressRecordRepository).save(progressRecord);
            verify(progressRecordMapper, never()).toResponseDTO(any());
        }
    }

    @Nested
    @DisplayName("getProgressRecordsInDateRange")
    class GetProgressRecordsInDateRange {

        @Test
        @DisplayName("Should throw BadRequestException when the start date is after the end date")
        void shouldThroWhenDatesAreInverted() {
            LocalDate from = LocalDate.now().plusDays(30);
            LocalDate to = LocalDate.now();

            assertThatThrownBy(() -> progressRecordService.getProgressRecordsInDateRange(
                            USER_ID, from, to, mock(Pageable.class)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("The start date cannot be after the end date");

            verifyNoInteractions(progressRecordRepository);
        }

        @Test
        @DisplayName("Should return the progress records recorded between the given dates")
        void shouldReturnProgressRecordsInDateRange() {
            LocalDate from = LocalDate.of(2026, 8, 1);
            LocalDate to = LocalDate.of(2026, 8, 25);

            Pageable pageable = PageRequest.of(0, 10);

            Page<ProgressRecord> progressRecords = new PageImpl<>(List.of(progressRecord), pageable, 1);

            when(progressRecordRepository.findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                            USER_ID, from, to, pageable))
                    .thenReturn(progressRecords);

            when(progressRecordMapper.toResponseDTO(progressRecord)).thenReturn(responseDTO);

            Page<ProgressRecordResponseDTO> result =
                    progressRecordService.getProgressRecordsInDateRange(USER_ID, from, to, pageable);

            assertThat(result.getContent()).containsExactly(responseDTO);

            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(progressRecordRepository)
                    .findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER_ID, from, to, pageable);

            verify(progressRecordMapper).toResponseDTO(progressRecord);
        }

        @Test
        @DisplayName("Should allow a date range containing a single day")
        void shouldAllowSingleDayRange() {
            LocalDate date = LocalDate.of(2026, 8, 25);
            Pageable pageable = PageRequest.of(0, 10);

            when(progressRecordRepository.findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                            USER_ID, date, date, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<ProgressRecordResponseDTO> result =
                    progressRecordService.getProgressRecordsInDateRange(USER_ID, date, date, pageable);

            assertThat(result).isEmpty();

            verify(progressRecordRepository)
                    .findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER_ID, date, date, pageable);
        }
    }

    @Nested
    @DisplayName("deleteProgressRecord")
    class DeleteProgressRecord {
        @Test
        @DisplayName("Should delete progress record successfully")
        void shouldDeleteProgressRecordSuccessfully() {
            when(progressRecordRepository.existsByIdAndUserId(PROGRESS_RECORD_ID, USER_ID))
                    .thenReturn(true);

            progressRecordService.deleteProgressRecord(PROGRESS_RECORD_ID, USER_ID);

            verify(progressRecordRepository).existsByIdAndUserId(PROGRESS_RECORD_ID, USER_ID);
            verify(progressRecordRepository).deleteById(PROGRESS_RECORD_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when progress record does not exist")
        void shouldThrowWhenProgressRecordDoesNotExist() {
            when(progressRecordRepository.existsByIdAndUserId(PROGRESS_RECORD_ID, USER_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> progressRecordService.deleteProgressRecord(PROGRESS_RECORD_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Progress record not found");

            verify(progressRecordRepository).existsByIdAndUserId(PROGRESS_RECORD_ID, USER_ID);
            verify(progressRecordRepository, never()).deleteById(any());
        }
    }
}
