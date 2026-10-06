package dev.genesshoan.fitnesstrackerapi.exercise.application.ports.inbound;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.genesshoan.fitnesstrackerapi.exercise.dto.MuscleResponseDTO;

/**
 * Port for muscle catalog operations.
 *
 * <p>Provides read-only access to the muscle catalog, allowing muscles
 * to be retrieved for exercise filtering and training-related operations.
 */
public interface MuscleServicePort {

    /**
     * Retrieves a paginated list of all muscles.
     *
     * @param pageable pagination and sorting parameters
     * @return a page containing the available muscles
     */
    Page<MuscleResponseDTO> getMuscles(Pageable pageable);

    /**
     * Retrieves a muscle by its URL-friendly slug.
     *
     * @param slug the URL-friendly muscle identifier
     * @return the muscle matching the specified slug
     * @throws ResourceNotFoundException if no muscle exists with the given slug
     */
    MuscleResponseDTO getMuscleBySlug(String slug);
}
