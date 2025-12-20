package UserService.service;


import UserService.dao.UserDao;
import UserService.dao.UserDaoImpl;
import UserService.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;

    public UserService() {
        this.userDao = new UserDaoImpl();
    }

    public User createUser(String name, String email, Integer age) {
        // Валидация
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Некорректный email");
        }

        // Проверка уникальности email
        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }

        User user = new User(name, email, age);
        return userDao.save(user);
    }

    public Optional<User> getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный ID");
        }
        return userDao.findById(id);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public User updateUser(Long id, String name, String email, Integer age) {
        User user = userDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (name != null) {
            user.setName(name);
        }

        if (email != null && !email.equals(user.getEmail())) {
            if (userDao.existsByEmail(email)) {
                throw new IllegalArgumentException("Новый email уже занят");
            }
            user.setEmail(email);
        }

        if (age != null) {
            user.setAge(age);
        }

        return userDao.update(user);
    }

    public void deleteUser(Long id) {
        if (!userDao.findById(id).isPresent()) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        userDao.delete(id);
    }

    public List<User> searchUsersByName(String name) {
        return userDao.findByName(name);
    }

    public long getUserCount() {
        return userDao.count();
    }
}
