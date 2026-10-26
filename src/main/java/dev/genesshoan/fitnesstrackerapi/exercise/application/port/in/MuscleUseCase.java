package dev.genesshoan.fitnesstrackerapi.exercise.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.common.error.exception.ResourceNotFoundException;
import dev.genesshoan.fitnesstrackerapi.exercise.application.port.in.result.MuscleResult;

/**
 * Use case for reading the muscle catalog.
 *
 * <p>Implemented by the exercise module and consumed by the modules that filter
 * exercises by muscle or describe the target muscles of a movement.
 */
public interface MuscleUseCase {

    /**
     * Retrieves a paginated list of all muscles.
     *
     * @param pageable pagination and sorting parameters
     * @return a page containing the available muscles
     */
    Page<MuscleResult> getMuscles(Pageable pageable);

    /**
     * Retrieves a muscle by its URL-friendly slug.
     *
     * @param slug the URL-friendly muscle identifier
     * @return the muscle matching the specified slug
     * @throws ResourceNotFoundException if no muscle exists with the given slug
     */
    MuscleResult getMuscleBySlug(String slug);
}
