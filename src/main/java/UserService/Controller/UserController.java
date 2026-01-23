package UserService.Controller;

import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Управление пользователями",
        description = "API для работы с пользователями системы")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(
            summary = "Создать пользователя",
            description = "Создает нового пользователя в системе. " +
                    "Email должен быть уникальным."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public EntityModel<UserResponse> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse userResponse = userService.createUser(request);

        EntityModel<UserResponse> resource = EntityModel.of(userResponse);

        resource.add(linkTo(methodOn(UserController.class).getUserById(userResponse.getId())).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(userResponse.getId(), null)).withRel("update"));
        resource.add(Link.of(linkTo(UserController.class).slash("delete").slash(userResponse.getId()).toString(), "delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));

        return resource;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить пользователя по ID",
            description = "Возвращает информацию о пользователе по его идентификатору"
    )
    public EntityModel<UserResponse> getUserById(
            @Parameter(description = "ID пользователя", example = "3")
            @PathVariable Long id) {

        UserResponse userResponse = userService.getUserById(id);


        EntityModel<UserResponse> resource = EntityModel.of(userResponse);


        resource.add(linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"));
        resource.add(Link.of(linkTo(UserController.class).slash("delete").slash(id).toString(), "delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
        resource.add(linkTo(methodOn(UserController.class).searchUsersByName("")).withRel("search"));

        return resource;
    }

    @GetMapping
    @Operation(
            summary = "Получить всех пользователей ",
            description = "Возвращает информацию о всех пользователях"
    )
    public CollectionModel<EntityModel<UserResponse>> getAllUsers() {

        List<EntityModel<UserResponse>> users = userService.getAllUsers().stream()
                .map(user -> {
                    EntityModel<UserResponse> resource = EntityModel.of(user);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(user.getId())).withSelfRel());
                    resource.add(linkTo(methodOn(UserController.class).updateUser(user.getId(), null)).withRel("update"));
                    resource.add(Link.of(linkTo(UserController.class).slash("delete").slash(user.getId()).toString(), "delete"));
                    return resource;
                })
                .collect(Collectors.toList());


        CollectionModel<EntityModel<UserResponse>> collectionModel = CollectionModel.of(users);

        collectionModel.add(linkTo(methodOn(UserController.class).getAllUsers()).withSelfRel());
        collectionModel.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create"));
        collectionModel.add(linkTo(methodOn(UserController.class).searchUsersByName("")).withRel("search"));

        return collectionModel;
    }

    @PutMapping("/update/{id}")
    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет информацию о пользователе по указанному ID"
    )
    public EntityModel<UserResponse> updateUser(
            @Parameter(description = "ID пользователя для обновления", example = "3")
            @PathVariable Long id,
            @Parameter(description = "Обновленные данные пользователя", required = true)
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse userResponse = userService.updateUser(id, request);

        EntityModel<UserResponse> resource = EntityModel.of(userResponse);

        resource.add(linkTo(methodOn(UserController.class).getUserById(id)).withSelfRel());
        resource.add(linkTo(methodOn(UserController.class).updateUser(id, null)).withRel("update"));
        resource.add(Link.of(linkTo(UserController.class).slash("delete").slash(id).toString(), "delete"));
        resource.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));

        return resource;
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя по указанному ID. Операция является необратимой."
    )
    public void deleteUser(
            @Parameter(description = "ID пользователя для удаления", example = "3")
            @PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Поиск пользователей по имени",
            description = "Ищет пользователей по частичному совпадению имени (fullName). " +
                    "Поиск не чувствителен к регистру."
    )
    public CollectionModel<EntityModel<UserResponse>> searchUsersByName(
            @Parameter(description = "Имя или часть имени для поиска", required = true, example = "John")
            @RequestParam String name) {

        List<EntityModel<UserResponse>> users = userService.searchUsersByName(name).stream()
                .map(user -> {
                    EntityModel<UserResponse> resource = EntityModel.of(user);
                    resource.add(linkTo(methodOn(UserController.class).getUserById(user.getId())).withSelfRel());
                    return resource;
                })
                .collect(Collectors.toList());

        CollectionModel<EntityModel<UserResponse>> collectionModel = CollectionModel.of(users);

        collectionModel.add(linkTo(methodOn(UserController.class).searchUsersByName(name)).withSelfRel());
        collectionModel.add(linkTo(methodOn(UserController.class).getAllUsers()).withRel("all-users"));
        collectionModel.add(linkTo(methodOn(UserController.class).createUser(null)).withRel("create"));

        return collectionModel;
    }

    @GetMapping("/count")
    @Operation(
            summary = "Получить количество пользователей",
            description = "Возвращает общее количество пользователей в системе."
    )
    public Long getUsersCount() {
        return userService.getUserCount();
    }



}