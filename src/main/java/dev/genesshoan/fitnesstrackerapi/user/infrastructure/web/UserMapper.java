package dev.genesshoan.fitnesstrackerapi.user.infrastructure.web;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.command.RegisterUserCommand;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import dev.genesshoan.fitnesstrackerapi.user.infrastructure.web.dto.UserResponse;
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
    @Mapping(target = "emailVerified", ignore = true)
    User toEntity(RegisterUserCommand command);

    @Mapping(target = "profilePictureUrl", ignore = true)
    UserResponse toResponseDTO(User user);
}
