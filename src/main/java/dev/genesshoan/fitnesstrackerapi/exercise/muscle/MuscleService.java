package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.mapper.MuscleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MuscleService {

    private final MuscleRepository muscleRepository;
    private final MuscleMapper muscleMapper;

    public Page<MuscleResponseDTO> getMuscles(Pageable pageable) {

        log.debug("Fetching muscles page={} size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return muscleRepository.findAll(pageable).map(muscleMapper::toResponseDTO);
    }

    public MuscleResponseDTO getMuscleBySlug(String slug) {

        log.info("Fetching muscle by slug={}", slug);

        var muscle = muscleRepository.findBySlug(slug)
                .orElseThrow(() -> {

                    log.warn("Muscle not found with id={}", slug);
                    return new ResourceNotFoundException("Muscle with slug " + slug + " not found");
                });

        return muscleMapper.toResponseDTO(muscle);
    }
}
