package dev.genesshoan.fitnesstrackerapi.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecord;
import dev.genesshoan.fitnesstrackerapi.progressrecord.ProgressRecordRepository;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.ProgressRecordBuilder;
import dev.genesshoan.fitnesstrackerapi.testdata.builder.UserBuilder;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProgressRecordRepositoryTest extends AbstractPostgresTest {

    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();

    private static final UUID PG_ID_1 = UUID.randomUUID();
    private static final UUID PG_ID_2 = UUID.randomUUID();
    private static final UUID PG_ID_3 = UUID.randomUUID();
    private static final UUID PG_ID_4 = UUID.randomUUID();
    private static final UUID PG_ID_5 = UUID.randomUUID();
    private static final UUID PG_ID_6 = UUID.randomUUID();

    @Autowired
    private ProgressRecordRepository progressRecordRepository;

    @Autowired
    private TestEntityFactory testEntityFactory;

    private User user1;

    @BeforeAll
    void setup() {
        user1 = testEntityFactory.createAndPersistUser(
                UserBuilder.aUser(testEntityFactory.faker()).withId(USER_ID_1));

        User user2 = testEntityFactory.createAndPersistUser(
                UserBuilder.aUser(testEntityFactory.faker()).withId(USER_ID_2));

        LocalDate baseDate = LocalDate.of(2026, 8, 26);

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_1)
                        .forUser(user1)
                        .withRecordedAt(baseDate.minusDays(5)));

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_2)
                        .forUser(user1)
                        .withRecordedAt(baseDate.minusDays(3)));

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_3)
                        .forUser(user1)
                        .withRecordedAt(baseDate.minusDays(1)));

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_4)
                        .forUser(user2)
                        .withRecordedAt(baseDate.minusDays(4)));

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_5)
                        .forUser(user2)
                        .withRecordedAt(baseDate.minusDays(2)));

        testEntityFactory.createAndPersistProgressRecord(
                ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                        .withId(PG_ID_6)
                        .forUser(user2)
                        .withRecordedAt(baseDate));
    }

    @Nested
    @DisplayName("findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc")
    class FindAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc {

        @Test
        @DisplayName("Should return the user registries between the provided date range")
        void shouldReturnUserProgressRecordsBetweenDateRange() {
            Page<ProgressRecord> result =
                    progressRecordRepository.findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                            USER_ID_1, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 27), Pageable.unpaged());

            assertThat(result.getContent())
                    .extracting(ProgressRecord::getId)
                    .containsExactly(PG_ID_1, PG_ID_2, PG_ID_3);
        }

        @Test
        @DisplayName("Should use pagination correctly")
        void shouldUsePaginationCorrectly() {
            Pageable pageable = PageRequest.of(0, 2);

            Page<ProgressRecord> result =
                    progressRecordRepository.findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                            USER_ID_1, LocalDate.of(2026, 8, 19), LocalDate.of(2026, 8, 25), pageable);

            assertThat(result.getContent()).extracting(ProgressRecord::getId).containsExactly(PG_ID_1, PG_ID_2);
        }

        @Test
        @DisplayName("Should not return other users' records")
        void shouldNotReturnOtherUsersRecords() {
            Page<ProgressRecord> result =
                    progressRecordRepository.findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                            USER_ID_1, LocalDate.of(2026, 8, 19), LocalDate.of(2026, 8, 27), Pageable.unpaged());

            assertThat(result.getContent()).extracting(ProgressRecord::getId).doesNotContain(PG_ID_4, PG_ID_5, PG_ID_6);
        }
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("Should throw DataIntegrityViolationException when user already has a record for the date")
        void shouldThrowWhenProgressRecordAlreadyExistsForUserAndDate() {
            ProgressRecord duplicate = ProgressRecordBuilder.aProgressRecord(testEntityFactory.faker())
                    .forUser(user1)
                    .withRecordedAt(LocalDate.of(2026, 8, 21))
                    .build();

            assertThatThrownBy(() -> progressRecordRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
