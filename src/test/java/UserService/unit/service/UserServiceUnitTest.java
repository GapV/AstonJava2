package UserService.unit.service;

import UserService.dao.UserDao;
import UserService.entity.User;
import UserService.service.UserService;
import UserService.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createTestUser(1L, "test@example.com");
    }

    @Test
    @DisplayName("Service: Успешное создание пользователя")
    void createUser_shouldCreateUserSuccessfully() {

        when(userDao.existsByEmail("new@example.com")).thenReturn(false);
        when(userDao.save(any(User.class))).thenReturn(testUser);

        User createdUser = userService.createUser("Test User", "new@example.com", 25);

        assertNotNull(createdUser);
        assertEquals("Test User", createdUser.getName());
        assertEquals("test@example.com", createdUser.getEmail());

        verify(userDao).existsByEmail("new@example.com");
        verify(userDao).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Создание пользователя - проверка уникальности email")
    void createUser_shouldThrowExceptionWhenEmailAlreadyExists() {

        when(userDao.existsByEmail("existing@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Test", "existing@example.com", 25)
        );

        assertEquals("Пользователь с таким email уже существует", exception.getMessage());
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Создание пользователя - валидация имени")
    void createUser_shouldValidateName() {

        IllegalArgumentException emptyNameException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("", "test@example.com", 25)
        );
        assertEquals("Имя не может быть пустым", emptyNameException.getMessage());


        IllegalArgumentException whitespaceNameException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("   ", "test@example.com", 25)
        );
        assertEquals("Имя не может быть пустым", whitespaceNameException.getMessage());


        IllegalArgumentException nullNameException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(null, "test@example.com", 25)
        );
        assertEquals("Имя не может быть пустым", nullNameException.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Создание пользователя - валидация email")
    void createUser_shouldValidateEmail() {

        IllegalArgumentException invalidEmailException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Test", "not-an-email", 25)
        );
        assertEquals("Некорректный email", invalidEmailException.getMessage());


        IllegalArgumentException nullEmailException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser("Test", null, 25)
        );
        assertEquals("Некорректный email", nullEmailException.getMessage());

        verify(userDao, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Получение пользователя по ID")
    void getUserById_shouldReturnUserWhenExists() {

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));


        Optional<User> result = userService.getUserById(1L);


        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        verify(userDao).findById(1L);
    }

    @Test
    @DisplayName("Service: Получение пользователя по несуществующему ID")
    void getUserById_shouldReturnEmptyWhenUserNotExists() {

        when(userDao.findById(999L)).thenReturn(Optional.empty());


        Optional<User> result = userService.getUserById(999L);


        assertFalse(result.isPresent());
        verify(userDao).findById(999L);
    }

    @Test
    @DisplayName("Service: Получение пользователя по ID - валидация ID")
    void getUserById_shouldValidateId() {

        IllegalArgumentException nullIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(null)
        );
        assertNotNull(nullIdException.getMessage());


        IllegalArgumentException negativeIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(-1L)
        );
        assertFalse(negativeIdException.getMessage().contains("ID должен быть положительным"));


        IllegalArgumentException zeroIdException = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(0L)
        );
        assertFalse(zeroIdException.getMessage().contains("ID должен быть положительным"));

        verify(userDao, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Service: Получение всех пользователей")
    void getAllUsers_shouldReturnAllUsers() {

        List<User> users = Arrays.asList(
                TestDataFactory.createTestUser(1L, "user1@example.com"),
                TestDataFactory.createTestUser(2L, "user2@example.com")
        );
        when(userDao.findAll()).thenReturn(users);


        List<User> result = userService.getAllUsers();


        assertThat(result)
                .hasSize(2)
                .extracting(User::getEmail)
                .containsExactly("user1@example.com", "user2@example.com");

        verify(userDao).findAll();
    }

    @Test
    @DisplayName("Service: Обновление пользователя")
    void updateUser_shouldUpdateUserSuccessfully() {

        User existingUser = TestDataFactory.createTestUser(1L, "old@example.com");
        existingUser.setName("Old Name");
        existingUser.setAge(25);

        User updatedUser = TestDataFactory.createTestUser(1L, "new@example.com");
        updatedUser.setName("New Name");
        updatedUser.setAge(30);

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByEmail("new@example.com")).thenReturn(false);
        when(userDao.update(any(User.class))).thenReturn(updatedUser);


        User result = userService.updateUser(1L, "New Name", "new@example.com", 30);


        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals(30, result.getAge());

        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("new@example.com");
        verify(userDao).update(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя - пользователь не найден")
    void updateUser_shouldThrowExceptionWhenUserNotFound() {

        when(userDao.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(999L, "New Name", "new@example.com", 30)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userDao, never()).update(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя - email уже занят другим пользователем")
    void updateUser_shouldThrowExceptionWhenEmailAlreadyTaken() {

        User existingUser = TestDataFactory.createTestUser(1L, "current@example.com");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByEmail("taken@example.com")).thenReturn(true);


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, "New Name", "taken@example.com", 30)
        );

        assertEquals("Новый email уже занят", exception.getMessage());
        verify(userDao, never()).update(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя - частичное обновление")
    void updateUser_shouldAllowPartialUpdate() {

        User existingUser = TestDataFactory.createTestUser(1L, "current@example.com");
        existingUser.setName("Old Name");
        existingUser.setAge(25);

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));

        when(userDao.update(any(User.class))).thenAnswer(invocation ->
                invocation.getArgument(0));


        User result = userService.updateUser(1L, "New Name", null, null);


        assertEquals("New Name", result.getName());
        assertEquals("current@example.com", result.getEmail()); // email не изменился
        assertEquals(25, result.getAge()); // возраст не изменился

        verify(userDao, never()).existsByEmail(anyString());
    }

    @Test
    @DisplayName("Service: Удаление пользователя")
    void deleteUser_shouldDeleteUserSuccessfully() {

        when(userDao.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userDao).delete(1L);


        userService.deleteUser(1L);


        verify(userDao).findById(1L);
        verify(userDao).delete(1L);
    }

    @Test
    @DisplayName("Service: Удаление несуществующего пользователя")
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {

        when(userDao.findById(999L)).thenReturn(Optional.empty());


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.deleteUser(999L)
        );

        assertEquals("Пользователь не найден", exception.getMessage());
        verify(userDao, never()).delete(anyLong());
    }

    @Test
    @DisplayName("Service: Поиск пользователей по имени")
    void searchUsersByName_shouldReturnMatchingUsers() {

        List<User> users = Arrays.asList(
                TestDataFactory.createUserWithParams("John Doe", "john1@example.com", 30),
                TestDataFactory.createUserWithParams("John Smith", "john2@example.com", 25)
        );

        when(userDao.findByName("John")).thenReturn(users);


        List<User> result = userService.searchUsersByName("John");


        assertThat(result)
                .hasSize(2)
                .extracting(User::getName)
                .containsExactly("John Doe", "John Smith");

        verify(userDao).findByName("John");
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
    @DisplayName("Service: Создание пользователя без возраста")
    void createUser_shouldAllowNullAge() {

        when(userDao.existsByEmail("test@example.com")).thenReturn(false);
        when(userDao.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });


        User createdUser = userService.createUser("Test User", "test@example.com", null);


        assertNotNull(createdUser);
        assertEquals("Test User", createdUser.getName());
        assertEquals("test@example.com", createdUser.getEmail());
        assertNull(createdUser.getAge());

        verify(userDao).save(any(User.class));
    }

    @Test
    @DisplayName("Service: Обновление пользователя без изменений")
    void updateUser_shouldReturnSameUserWhenNoChanges() {

        User existingUser = TestDataFactory.createTestUser(1L, "test@example.com");
        existingUser.setName("Test User");
        existingUser.setAge(25);

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.update(any(User.class))).thenReturn(existingUser);


        User result = userService.updateUser(1L, "Test User", "test@example.com", 25);


        assertEquals(existingUser, result);
        verify(userDao).findById(1L);
        verify(userDao).update(any(User.class));
    }
}
