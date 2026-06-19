package dev.genesshoan.fitnesstrackerapi.exercise.muscle;

import dev.genesshoan.fitnesstrackerapi.exercise.muscle.domain.Muscle;
import dev.genesshoan.fitnesstrackerapi.exercise.muscle.dto.MuscleResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/muscles")
@Tag(name = "Muscles", description = "Endpoints for managing and retrieving muscles")
public class MuscleController {

    private final MuscleService muscleService;

    @Operation(
            summary = "Gets all muscles",
            description = "Returns a paginated list of all muscles"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Muscles retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MuscleResponseDTO.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<Page<MuscleResponseDTO>> getMuscles(@ParameterObject Pageable pageable) {

        Page<MuscleResponseDTO> muscles = muscleService.getMuscles(pageable);

        return ResponseEntity.ok(muscles);
    }

    @Operation(
            summary = "Gets a muscle by its slug",
            description = "Returns a muscle by its slug"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Muscle retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MuscleResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Muscle not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation =  MuscleResponseDTO.class)
                    )
            )
    })
    @GetMapping("/{slug}")
    public ResponseEntity<MuscleResponseDTO> getMuscle(@PathVariable String slug) {

        var muscle = muscleService.getMuscleBySlug(slug);

        return ResponseEntity.ok(muscle);
    }
}
