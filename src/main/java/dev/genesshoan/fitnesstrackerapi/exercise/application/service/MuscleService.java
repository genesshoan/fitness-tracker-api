package dev.genesshoan.fitnesstrackerapi.exercise.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.application.mapper.MuscleResultMapper;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.MuscleUseCase;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleResult;
import dev.genesshoan.fitnesstrackerapi.exercise.infrastructure.persistence.MuscleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for muscle catalog operations.
 *
 * <p>Provides read-only access to the muscle catalog, organized by
 * body region. Muscles are used to filter exercises and track
 * target/stabilizer relationships in training sessions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MuscleService implements MuscleUseCase {

    private final MuscleRepository muscleRepository;
    private final MuscleResultMapper muscleResultMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<MuscleResult> getMuscles(Pageable pageable) {

        log.debug("Fetching muscles page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return muscleRepository.findAll(pageable).map(muscleResultMapper::toResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleResult getMuscleBySlug(String slug) {

        log.info("Fetching muscle by slug={}", slug);

        var muscle = muscleRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Muscle not found with slug={}", slug);
            return new ResourceNotFoundException("Muscle with slug " + slug + " not found");
        });

        return muscleResultMapper.toResult(muscle);
    }
}
