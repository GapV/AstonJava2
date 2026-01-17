package UserService.integration.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.kafka.UserEventProducer;
import UserService.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@DisplayName("Интеграционные тесты UserService с Testcontainers")
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15")
    )
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserDao userDao;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @BeforeEach
    void setUp() {
        userDao.deleteAll();
        reset(userEventProducer);
    }

    @Test
    @DisplayName("Создание пользователя: успешное создание и отправка события")
    void createUser_shouldSaveUserAndSendEvent() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Иван Иванов");
        request.setEmail("ivan@example.com");
        request.setAge(30);

        UserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Иван Иванов");
        assertThat(response.getEmail()).isEqualTo("ivan@example.com");
        assertThat(response.getAge()).isEqualTo(30);

        Optional<User> savedUser = userDao.findById(response.getId());
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getName()).isEqualTo("Иван Иванов");

        verify(userEventProducer).sendUserCreatedEvent(
                eq(response.getId()),
                eq("Иван Иванов"),
                eq("ivan@example.com")
        );
    }

    @Test
    @DisplayName("Создание пользователя: дубликат email должен выбрасывать исключение")
    void createUser_duplicateEmail_shouldThrowException() {

        CreateUserRequest request1 = new CreateUserRequest();
        request1.setName("Иван Иванов");
        request1.setEmail("ivan@example.com");
        request1.setAge(30);
        userService.createUser(request1);

        CreateUserRequest request2 = new CreateUserRequest();
        request2.setName("Петр Петров");
        request2.setEmail("ivan@example.com");
        request2.setAge(25);

        assertThatThrownBy(() -> userService.createUser(request2))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Не удалось сохранить пользователя");

        verify(userEventProducer, times(1)).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Получение пользователя по ID: успешное получение")
    void getUserById_shouldReturnUser() {

        User user = new User();
        user.setName("Мария Сидорова");
        user.setEmail("maria@example.com");
        user.setAge(28);
        User savedUser = userDao.save(user);

        UserResponse response = userService.getUserById(savedUser.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedUser.getId());
        assertThat(response.getName()).isEqualTo("Мария Сидорова");
        assertThat(response.getEmail()).isEqualTo("maria@example.com");
    }

    @Test
    @DisplayName("Получение пользователя по ID: несуществующий ID должен выбрасывать исключение")
    void getUserById_nonExistentId_shouldThrowException() {

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка при поиске пользователя");
    }

    @Test
    @DisplayName("Получение всех пользователей")
    void getAllUsers_shouldReturnAllUsers() {

        User user1 = new User();
        user1.setName("Алексей Петров");
        user1.setEmail("alex@example.com");
        user1.setAge(35);

        User user2 = new User();
        user2.setName("Ольга Смирнова");
        user2.setEmail("olga@example.com");
        user2.setAge(29);

        userDao.save(user1);
        userDao.save(user2);

        List<UserResponse> users = userService.getAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::getEmail)
                .containsExactlyInAnyOrder("alex@example.com", "olga@example.com");
    }

    @Test
    @DisplayName("Обновление пользователя: успешное обновление")
    void updateUser_shouldUpdateUserSuccessfully() {

        User user = new User();
        user.setName("Старое Имя");
        user.setEmail("old@example.com");
        user.setAge(25);
        User savedUser = userDao.save(user);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Новое Имя");
        request.setEmail("new@example.com");
        request.setAge(30);

        UserResponse response = userService.updateUser(savedUser.getId(), request);

        assertThat(response.getName()).isEqualTo("Новое Имя");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getAge()).isEqualTo(30);

        User updatedUser = userDao.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("Новое Имя");
        assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("Обновление пользователя: дубликат email должен выбрасывать исключение")
    void updateUser_duplicateEmail_shouldThrowException() {

        User user1 = new User();
        user1.setName("Первый Пользователь");
        user1.setEmail("first@example.com");
        user1.setAge(25);
        userDao.save(user1);

        User user2 = new User();
        user2.setName("Второй Пользователь");
        user2.setEmail("second@example.com");
        user2.setAge(30);
        User savedUser2 = userDao.save(user2);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("first@example.com"); // Дубликат email

        assertThatThrownBy(() -> userService.updateUser(savedUser2.getId(), request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Новый email уже занят");
    }

    @Test
    @DisplayName("Удаление пользователя: успешное удаление и отправка события")
    void deleteUser_shouldDeleteUserAndSendEvent() {

        User user = new User();
        user.setName("Удаляемый Пользователь");
        user.setEmail("delete@example.com");
        user.setAge(40);
        User savedUser = userDao.save(user);

        userService.deleteUser(savedUser.getId());


        Optional<User> deletedUser = userDao.findById(savedUser.getId());
        assertThat(deletedUser).isEmpty();

        verify(userEventProducer).sendUserDeletedEvent(
                eq(savedUser.getId()),
                eq("Удаляемый Пользователь"),
                eq("delete@example.com")
        );
    }

    @Test
    @DisplayName("Поиск пользователя по имени")
    void searchUsersByName_shouldReturnMatchingUsers() {

        User user1 = new User();
        user1.setName("Иван Иванов");
        user1.setEmail("ivan1@example.com");
        user1.setAge(30);

        userDao.save(user1);

        List<UserResponse> results = userService.searchUsersByName("Иван Иванов");

        assertThat(results).hasSize(1);
        assertThat(results).extracting(UserResponse::getName)
                .containsExactlyInAnyOrder("Иван Иванов");
    }

    @Test
    @DisplayName("Подсчет пользователей")
    void getUserCount_shouldReturnCorrectCount() {

        for (int i = 0; i < 5; i++) {
            User user = new User();
            user.setName("Пользователь " + i);
            user.setEmail("user" + i + "@example.com");
            user.setAge(20 + i);
            userDao.save(user);
        }

        long count = userService.getUserCount();

        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("Транзакционность: откат при ошибке в Kafka")
    void createUser_kafkaError_shouldRollbackTransaction() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Транзакционный Пользователь");
        request.setEmail("transaction@example.com");
        request.setAge(25);

        doThrow(new RuntimeException("Kafka недоступен"))
                .when(userEventProducer).sendUserCreatedEvent(anyLong(), anyString(), anyString());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Не удалось сохранить пользователя");

        List<User> allUsers = userDao.findAll();
        assertThat(allUsers).isEmpty();
    }

    @Test
    @DisplayName("Частичное обновление пользователя")
    void updateUser_partialUpdate_shouldUpdateOnlyProvidedFields() {

        User user = new User();
        user.setName("Исходное Имя");
        user.setEmail("original@example.com");
        user.setAge(25);
        User savedUser = userDao.save(user);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Обновленное Имя");

        UserResponse response = userService.updateUser(savedUser.getId(), request);

        assertThat(response.getName()).isEqualTo("Обновленное Имя");
        assertThat(response.getEmail()).isEqualTo("original@example.com"); // Осталось прежним
        assertThat(response.getAge()).isEqualTo(25); // Осталось прежним
    }

    @Test
    @DisplayName("Поиск по пустому имени должен возвращать пустой список")
    void searchUsersByName_emptyName_shouldReturnEmptyList() {

        User user = new User();
        user.setName("Тестовый Пользователь");
        user.setEmail("test@example.com");
        user.setAge(30);
        userDao.save(user);

        List<UserResponse> results = userService.searchUsersByName("НесуществующееИмя");

        assertThat(results).isEmpty();
    }
}