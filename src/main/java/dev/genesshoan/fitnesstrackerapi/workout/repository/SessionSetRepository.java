package dev.genesshoan.fitnesstrackerapi.workout.repository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {}
