package UserService.util;

import UserService.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestDataFactory {

    private TestDataFactory() {
    }

    public static User createTestUser() {
        return createTestUser("test@example.com");
    }

    public static User createTestUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setAge(25);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    public static User createTestUser(Long id, String email) {
        User user = createTestUser(email);
        user.setId(id);
        return user;
    }

    public static List<User> createTestUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(createTestUser((long) i, "user" + i + "@example.com"));
        }
        return users;
    }

    public static User createUserWithParams(String name, String email, Integer age) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setAge(age);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
