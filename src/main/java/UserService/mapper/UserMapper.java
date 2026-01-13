package UserService.mapper;

import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity (CreateUserRequest request);

    UserResponse toResponse(User user);
    List<UserResponse> listToResponse(List<User> userList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

}
