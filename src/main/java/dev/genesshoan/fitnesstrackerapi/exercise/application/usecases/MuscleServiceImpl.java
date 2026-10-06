package dev.genesshoan.fitnesstrackerapi.exercise.application.usecases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound.MuscleServicePort;
import dev.genesshoan.fitnesstrackerapi.exercise.application.ports.outbound.MuscleRepositoryPort;
import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleResponseDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.mapper.MuscleMapper;
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
public class MuscleServiceImpl implements MuscleServicePort {

    private final MuscleRepositoryPort muscleRepository;
    private final MuscleMapper muscleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<MuscleResponseDTO> getMuscles(Pageable pageable) {

        log.debug("Fetching muscles page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return muscleRepository.findAll(pageable).map(muscleMapper::toResponseDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuscleResponseDTO getMuscleBySlug(String slug) {

        log.info("Fetching muscle by slug={}", slug);

        var muscle = muscleRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Muscle not found with id={}", slug);
            return new ResourceNotFoundException("Muscle with slug " + slug + " not found");
        });

        return muscleMapper.toResponseDTO(muscle);
    }
}
