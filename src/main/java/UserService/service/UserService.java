package UserService.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserDao userDao;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        try {
            if (userDao.existsByEmail(request.getEmail())) {
                throw new Exception ("Пользователь с таким email уже существует");
            }

            User user = userMapper.toEntity(request);
            User savedUser = userDao.save(user);
            log.info("Пользователь сохранен: {}", user.getEmail());
            return userMapper.toResponse(savedUser);
        } catch (Exception e) {
            log.error("Ошибка при сохранении пользователя", e);
            throw new RuntimeException("Не удалось сохранить пользователя", e);
        }
    }

    public UserResponse getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный ID");
        }
        try {
            Optional<User> userOptional = userDao.findById(id);
            User user = userOptional.orElseThrow(() ->
                    new Exception("Пользователь не найден")
            );
            return userMapper.toResponse(user);
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователя по ID: {}", id, e);
            throw new RuntimeException("Ошибка при поиске пользователя", e);
        }
    }

    public List<UserResponse> getAllUsers() {
        try {
            List<User> users = userDao.findAll();
            return users.stream()
                    .map(userMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Ошибка при получении всех пользователей", e);
            throw new RuntimeException("Ошибка при получении пользователей", e);
        }
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        try {
            User user = userDao.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            if (request.getName() != null) {
                user.setName(request.getName());
            }

            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userDao.existsByEmail(request.getEmail())) {
                    throw new IllegalArgumentException("Новый email уже занят");
                }
                user.setEmail(request.getEmail());
            }

            if (request.getAge() != null) {
                user.setAge(request.getAge());
            }

            User updatedUser = userDao.save(user);
            log.info("Пользователь обновлен: {}", user.getEmail());
            return userMapper.toResponse(updatedUser);
        } catch (Exception e) {
            log.error("Ошибка при обновлении пользователя: {}", e);
            throw new RuntimeException("Не удалось обновить пользователя", e);
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        try {
            if (userDao.findById(id).isEmpty()) {
                log.warn("Пользователь с ID {} не найден", id);
                throw new IllegalArgumentException("Пользователь не найден");
            }
            userDao.deleteById(id);
            log.info("Пользователь удален: {}", id);
        } catch (Exception e) {
            log.error("Ошибка при удалении пользователя: {}", id, e);
            throw new RuntimeException("Не удалось удалить пользователя", e);
        }
    }

    public List<UserResponse> searchUsersByName(String name) {
        try {
            List<User> users = userDao.findByName(name);
            return users.stream()
                    .map(userMapper::toResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Ошибка при поиске пользователей по имени: {}", name, e);
            throw new RuntimeException("Ошибка при поиске пользователей", e);
        }
    }

    public long getUserCount() {
        try {
            return userDao.count();
        } catch (Exception e) {
            log.error("Ошибка при подсчете пользователей", e);
            throw new RuntimeException("Ошибка при подсчете пользователей", e);
        }
    }
}
