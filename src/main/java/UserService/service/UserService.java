package UserService.service;


import UserService.dao.UserDao;
import UserService.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Transactional
    public User createUser(String name, String email, Integer age) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Имя не может быть пустым");
            }

            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("Некорректный email");
            }

            if (userDao.existsByEmail(email)) {
                throw new IllegalArgumentException("Пользователь с таким email уже существует");
            }

            User user = new User(name, email, age);
            logger.info("Пользователь сохранен: {}", user.getEmail());
            return userDao.save(user);
        } catch (Exception e) {
            logger.error("Ошибка при сохранении пользователя", e);
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    public Optional<User> getUserById(Long id) {

            if (id == null || id <= 0) {
                throw new IllegalArgumentException("Некорректный ID");
            }
        try {
            return userDao.findById(id);
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователя по ID: {}", id, e);
            throw new RuntimeException("Ошибка при поиске пользователя", e);
        }
    }

    public List<User> getAllUsers() {
        try {
            return userDao.findAll();
        } catch (Exception e) {
            logger.error("Ошибка при получении всех пользователей", e);
            throw new RuntimeException("Ошибка при получении пользователей", e);
        }
    }

    @Transactional
    public User updateUser(Long id, String name, String email, Integer age) {
        try {
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
            logger.info("Пользователь обновлен: {}", user.getEmail());
            return userDao.save(user);
        } catch (Exception e) {
            logger.error("Ошибка при обновлении пользователя: {}", email, e);
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        try {
            if (userDao.findById(id).isEmpty()) {
                logger.warn("Пользователь с ID {} не найден", id);
                throw new IllegalArgumentException("Пользователь не найден");
            }
            userDao.deleteById(id);
            logger.info("Пользователь удален: {}", id);
        } catch (Exception e) {
            logger.error("Ошибка при удалении пользователя: {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    public List<User> searchUsersByName( String name) {
        try {
            return userDao.findByName(name);
        } catch (Exception e) {
            logger.error("Ошибка при поиске пользователей по имени: {}", name, e);
            throw new RuntimeException("Ошибка при поиске пользователей", e);
        }
    }

    public long getUserCount() {
        try {
            return userDao.count();
        } catch (Exception e) {
            logger.error("Ошибка при подсчете пользователей", e);
            throw new RuntimeException("Ошибка при подсчете пользователей", e);
        }
    }
}
