package UserService.Controller;

import UserService.entity.User;
import UserService.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public void createUser(@RequestBody User user){
        userService.createUser(user.getName(),user.getEmail(), user.getAge());
    }

    @GetMapping("/{id}")
    public Optional<User> getUserById(@PathVariable Long id){
        return userService.getUserById(id);
    }

    @GetMapping
    public List<User> allUsers(){
          return userService.getAllUsers();
    }

    @PostMapping("/update")
    public void updateUser(@RequestBody User user){
        userService.updateUser(user.getId(),user.getName(), user.getEmail(), user.getAge());
    }

    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
    }

    @GetMapping("/searchbyname/{name}")
    public List<User> searchUsersByName(@PathVariable String name){
        return userService.searchUsersByName(name);
    }

    @GetMapping("/count")
    public long UsersCount(){
        return userService.getUserCount();
    }

}
