package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.mapper.MuscleMapper;
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
public class MuscleService {

    private final MuscleRepository muscleRepository;
    private final MuscleMapper muscleMapper;

    /**
     * Retrieves a paginated list of all muscles.
     *
     * <p>Muscles are organized by {@link dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.BodyRegion}
     * and can be used to filter exercises by target muscle groups.
     *
     * @param pageable pagination parameters (page number, size, sort)
     * @return a {@link Page} of {@link MuscleResponseDTO} objects
     */
    public Page<MuscleResponseDTO> getMuscles(Pageable pageable) {

        log.debug("Fetching muscles page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        return muscleRepository.findAll(pageable).map(muscleMapper::toResponseDTO);
    }

    /**
     * Retrieves a single muscle by its URL-friendly slug.
     *
     * <p>The slug is a URL-friendly identifier (e.g., "bicep", "quadriceps").
     *
     * @param slug the URL-friendly muscle identifier
     * @return a {@link MuscleResponseDTO} with muscle details
     * @throws ResourceNotFoundException if no muscle exists with the given slug
     */
    public MuscleResponseDTO getMuscleBySlug(String slug) {

        log.info("Fetching muscle by slug={}", slug);

        var muscle = muscleRepository.findBySlug(slug).orElseThrow(() -> {
            log.warn("Muscle not found with id={}", slug);
            return new ResourceNotFoundException("Muscle with slug " + slug + " not found");
        });

        return muscleMapper.toResponseDTO(muscle);
    }
}
