package UserService.Controller;

import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/update/{id}")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @GetMapping("/search")
    public List<UserResponse> searchUsersByName(@RequestParam String name) {
        return userService.searchUsersByName(name);
    }

    @GetMapping("/count")
    public Long getUsersCount() {
        return userService.getUserCount();
    }
}