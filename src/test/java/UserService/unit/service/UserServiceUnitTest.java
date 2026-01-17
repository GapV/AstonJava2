package UserService.unit.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.kafka.UserEventProducer;
import UserService.mapper.UserMapper;
import UserService.service.UserService;
import UserService.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser(1L, "test@example.com");
        testUserResponse = new UserResponse();
        testUserResponse.setId(1L);
        testUserResponse.setName("Test User");
        testUserResponse.setEmail("test@example.com");
        testUserResponse.setAge(25);
    }

    private CreateUserRequest createCreateUserRequest(String name, String email, Integer age) {
        CreateUserRequest request = new CreateUserRequest();
        request.setName(name);
        request.setEmail(email);
        request.setAge(age);
        return request;
    }

    private UpdateUserRequest createUpdateUserRequest(String name, String email, Integer age) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName(name);
        request.setEmail(email);
        request.setAge(age);
        return request;
    }

    private UserResponse createUserResponse(Long id, String name, String email, Integer age) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setName(name);
        response.setEmail(email);
        response.setAge(age);
        return response;
    }

    @Test
    @DisplayName("Service: Успешное создание пользователя")
    void createUser_shouldCreateUserSuccessfully() {
        CreateUserRequest request = createCreateUserRequest("Test User", "test@example.com", 25);

        when(userMapper.toEntity(request)).thenReturn(testUser);
        when(userDao.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());

        verify(userMapper).toEntity(request);
        verify(userDao).save(testUser);
        verify(userMapper).toResponse(testUser);

    }

    @Test
    @DisplayName("Service: Создание пользователя - проверка уникальности email")
    void createUser_shouldThrowExceptionWhenEmailAlreadyExists() {
        CreateUserRequest request = createCreateUserRequest("Test", "existing@example.com", 25);

        User mockUser = new User("Test", "existing@example.com", 25);

        when(userMapper.toEntity(request)).thenReturn(mockUser);

        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"users_email_key\""
        );
        when(userDao.save(mockUser)).thenThrow(dive);

        Exception exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("уже существует") ||
                exception.getMessage().contains("удалось сохранить"));

        verify(userMapper).toEntity(request);
        verify(userDao).save(mockUser);
        verify(userMapper, never()).toResponse(any());
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());

    }

    @Test
    @DisplayName("Service: Создание пользователя - валидация ID при получении")
    void getUserById_shouldValidateId() {

        IllegalArgumentException nullIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(null)
        );
        assertEquals("Некорректный ID", nullIdException.getMessage());

        IllegalArgumentException negativeIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(-1L)
        );
        assertEquals("Некорректный ID", negativeIdException.getMessage());

        IllegalArgumentException zeroIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(0L)
        );
        assertEquals("Некорректный ID", zeroIdException.getMessage());

        verify(userDao, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Service: Получение пользователя по ID")
    void getUserById_shouldReturnUserWhenExists() {

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);


        UserResponse result = userService.getUserById(1L);


        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        verify(userDao).findById(1L);
        verify(userMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("Service: Получение пользователя по несуществующему ID")
    void getUserById_shouldThrowExceptionWhenUserNotExists() {

        when(userDao.findById(999L)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(999L)
        );

        assertEquals("Ошибка при поиске пользователя", exception.getMessage());
        verify(userDao).findById(999L);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Service: Получение всех пользователей")
    void getAllUsers_shouldReturnAllUsers() {

        List<User> users = Arrays.asList(
                TestDataFactory.createTestUser(1L, "user1@example.com"),
                TestDataFactory.createTestUser(2L, "user2@example.com")
        );

        UserResponse userResponse1 = createUserResponse(1L, "User 1", "user1@example.com", 30);
        UserResponse userResponse2 = createUserResponse(2L, "User 2", "user2@example.com", 25);

        when(userDao.findAll()).thenReturn(users);
        when(userMapper.toResponse(users.get(0))).thenReturn(userResponse1);
        when(userMapper.toResponse(users.get(1))).thenReturn(userResponse2);


        List<UserResponse> result = userService.getAllUsers();


        assertThat(result)
                .hasSize(2)
                .extracting(UserResponse::getEmail)
                .containsExactly("user1@example.com", "user2@example.com");

        verify(userDao).findAll();
        verify(userMapper, times(2)).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя")
    void updateUser_shouldUpdateUserSuccessfully() {

        UpdateUserRequest request = createUpdateUserRequest("New Name", "new@example.com", 30);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("New Name");
        updatedUser.setEmail("new@example.com");
        updatedUser.setAge(30);

        UserResponse updatedResponse = createUserResponse(1L, "New Name", "new@example.com", 30);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.existsByEmail("new@example.com")).thenReturn(false);
        when(userDao.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);


        UserResponse result = userService.updateUser(1L, request);


        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals(30, result.getAge());

        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("new@example.com");
        verify(userDao).save(any(User.class));
        verify(userMapper).toResponse(updatedUser);
    }

    @Test
    @DisplayName("Service: Обновление пользователя - пользователь не найден")
    void updateUser_shouldThrowExceptionWhenUserNotFound() {

        UpdateUserRequest request = createUpdateUserRequest("New Name", "new@example.com", 30);

        when(userDao.findById(999L)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(999L, request)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userDao).findById(999L);
        verify(userDao, never()).save(any(User.class));
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Service: Обновление пользователя - email уже занят другим пользователем")
    void updateUser_shouldThrowExceptionWhenEmailAlreadyTaken() {

        UpdateUserRequest request = createUpdateUserRequest(null, "taken@example.com", null);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.existsByEmail("taken@example.com")).thenReturn(true);


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        assertEquals("Новый email уже занят", exception.getMessage());
        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("taken@example.com");
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя - частичное обновление")
    void updateUser_shouldAllowPartialUpdate() {

        testUser.setName("Old Name");
        testUser.setAge(25);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("New Name");
        updatedUser.setEmail("test@example.com");
        updatedUser.setAge(25);

        UserResponse updatedResponse = createUserResponse(1L, "New Name", "test@example.com", 25);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);


        UserResponse result = userService.updateUser(1L, request);


        assertEquals("New Name", result.getName());
        assertEquals("test@example.com", result.getEmail()); // email не изменился
        assertEquals(25, result.getAge()); // возраст не изменился

        verify(userDao).findById(1L);
        verify(userDao, never()).existsByEmail(anyString());
        verify(userDao).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Удаление пользователя")
    void deleteUser_shouldDeleteUserSuccessfully() {

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userDao).deleteById(1L);


        userService.deleteUser(1L);


        verify(userDao).findById(1L);
        verify(userDao).deleteById(1L);
    }

    @Test
    @DisplayName("Service: Удаление несуществующего пользователя")
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {

        when(userDao.findById(999L)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.deleteUser(999L)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userDao).findById(999L);
        verify(userDao, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Service: Поиск пользователей по имени")
    void searchUsersByName_shouldReturnMatchingUsers() {

        List<User> users = Arrays.asList(
                TestDataFactory.createUserWithParams("John Doe", "john1@example.com", 30),
                TestDataFactory.createUserWithParams("John Smith", "john2@example.com", 25)
        );

        UserResponse response1 = createUserResponse(1L, "John Doe", "john1@example.com", 30);
        UserResponse response2 = createUserResponse(2L, "John Smith", "john2@example.com", 25);

        when(userDao.findByName("John")).thenReturn(users);
        when(userMapper.toResponse(users.get(0))).thenReturn(response1);
        when(userMapper.toResponse(users.get(1))).thenReturn(response2);


        List<UserResponse> result = userService.searchUsersByName("John");


        assertThat(result)
                .hasSize(2)
                .extracting(UserResponse::getName)
                .containsExactly("John Doe", "John Smith");

        verify(userDao).findByName("John");
        verify(userMapper, times(2)).toResponse(any(User.class));
    }

    @Test
    @DisplayName("Service: Получение количества пользователей")
    void getUserCount_shouldReturnCorrectCount() {

        when(userDao.count()).thenReturn(5L);


        long count = userService.getUserCount();


        assertEquals(5L, count);
        verify(userDao).count();
    }


    @Test
    @DisplayName("Service: Обновление пользователя без изменений")
    void updateUser_shouldReturnSameUserWhenNoChanges() {

        UpdateUserRequest request = createUpdateUserRequest("Test User", "test@example.com", 25);

        testUser.setName("Test User");
        testUser.setAge(25);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(testUser)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);


        UserResponse result = userService.updateUser(1L, request);


        assertEquals(testUserResponse.getId(), result.getId());
        assertEquals(testUserResponse.getName(), result.getName());
        assertEquals(testUserResponse.getEmail(), result.getEmail());

        verify(userDao).findById(1L);
        verify(userDao).save(testUser);
        verify(userMapper).toResponse(testUser);
    }


    @Test
    @DisplayName("Service: Обновление пользователя - тот же email")
    void updateUser_shouldNotCheckEmailWhenSameEmailProvided() {

        testUser.setEmail("test@example.com");

        UpdateUserRequest request = createUpdateUserRequest("New Name", "test@example.com", 30);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("New Name");
        updatedUser.setEmail("test@example.com");
        updatedUser.setAge(30);

        UserResponse updatedResponse = createUserResponse(1L, "New Name", "test@example.com", 30);

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenReturn(updatedResponse);


        UserResponse result = userService.updateUser(1L, request);


        assertEquals("New Name", result.getName());
        assertEquals("test@example.com", result.getEmail());
        verify(userDao, never()).existsByEmail(anyString());
    }
}