package UserService.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.feignClient.NotificationClient;
import UserService.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import UserService.kafka.UserEventProducer;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserDao userDao;
    private final UserMapper userMapper;
    private final UserEventProducer userEventProducer;
    private final NotificationClient notificationClient;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        try {
            User user = userMapper.toEntity(request);
            User savedUser = userDao.save(user);
            log.info("Пользователь сохранен: {}", user.getEmail());

            // Отправляем событие в Kafka
            userEventProducer.sendUserCreatedEvent(
                    savedUser.getId(),
                    savedUser.getName(),
                    savedUser.getEmail()

            );
            return userMapper.toResponse(savedUser);
        } catch (Exception e) {
            if (e.getMessage().contains("23505")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой емайл уже есть");
            }
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
    }

    @Transactional
    public void deleteUser(Long id) {
            User user = userDao.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Пользователь с ID {} не найден", id);
                        return new IllegalArgumentException("Пользователь не найден");
                    });

            String userEmail = user.getEmail();
            String userName = user.getName();

            userDao.deleteById(id);
            log.info("Пользователь удален: {}", id);

            // Отправляем событие в Kafka
            userEventProducer.sendUserDeletedEvent(id, userName, userEmail);

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

    public String checkNotificationServiceHealth() {
        try {
            String healthResponse = notificationClient.healthCheck();
            log.info("Notification Service health check: {}", healthResponse);
            return healthResponse;
        } catch (Exception e) {
            log.error("Error checking notification service health: {}", e.getMessage());
            throw new RuntimeException("Notification service is unavailable", e);
        }
    }

}
