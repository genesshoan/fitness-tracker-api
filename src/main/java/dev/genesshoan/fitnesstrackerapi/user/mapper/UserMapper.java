package dev.genesshoan.fitnesstrackerapi.user.mapper;

import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.dto.UserResponseDTO;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "profilePictureKey", ignore = true)
    User toEntity(RegisterRequestDTO dto);

    @Mapping(target = "profilePictureUrl", ignore = true)
    UserResponseDTO toResponseDTO(User user);
}
