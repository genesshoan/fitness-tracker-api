package dev.genesshoan.fitnesstrackerapi.workout.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.genesshoan.fitnesstrackerapi.workout.domain.SessionSet;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {}
