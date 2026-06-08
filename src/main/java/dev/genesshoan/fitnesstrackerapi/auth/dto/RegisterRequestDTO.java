package dev.genesshoan.fitnesstrackerapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record RegisterRequestDTO(

        @NotBlank
        String username,

        @NotBlank
        @Length(min = 8)
        String password,

        @NotBlank
        @Email
        String email
) {
}
