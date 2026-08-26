package dev.genesshoan.fitnesstrackerapi.progressrecord;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgressRecordRepository extends JpaRepository<ProgressRecord, UUID> {

    Page<ProgressRecord> findAllByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
