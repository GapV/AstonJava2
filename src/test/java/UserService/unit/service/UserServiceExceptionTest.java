package UserService.unit.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.mapper.UserMapper;
import UserService.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceExceptionTest {

    @Mock
    private UserDao userDao;

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("Service: Обработка исключений DAO при создании пользователя")
    void createUser_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        when(userDao.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any())).thenReturn(new User());
        when(userDao.save(any())).thenThrow(new RuntimeException("Database connection failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));
        verify(userDao).existsByEmail("test@example.com");
        verify(userDao).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при получении пользователя")
    void getUserById_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        when(userDao.findById(anyLong())).thenThrow(new RuntimeException("Query failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(1L)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Ошибка при поиске пользователя"));
        verify(userDao).findById(1L);
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при получении всех пользователей")
    void getAllUsers_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        when(userDao.findAll()).thenThrow(new RuntimeException("Connection error"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getAllUsers()
        );

        // Assert
        assertTrue(exception.getMessage().contains("Ошибка при получении пользователей"));
        verify(userDao).findAll();
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при обновлении пользователя")
    void updateUser_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");

        when(userDao.findById(anyLong())).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenThrow(new RuntimeException("Update failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось обновить пользователя"));
        verify(userDao).findById(1L);
        verify(userDao).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при удалении пользователя")
    void deleteUser_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        User existingUser = new User();
        existingUser.setId(1L);

        when(userDao.findById(anyLong())).thenReturn(Optional.of(existingUser));
        doThrow(new RuntimeException("Delete failed")).when(userDao).deleteById(anyLong());

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.deleteUser(1L)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось удалить пользователя"));
        verify(userDao).findById(1L);
        verify(userDao).deleteById(1L);
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при поиске по имени")
    void searchUsersByName_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        when(userDao.findByName(anyString())).thenThrow(new RuntimeException("Search failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.searchUsersByName("John")
        );

        // Assert
        assertTrue(exception.getMessage().contains("Ошибка при поиске пользователей"));
        verify(userDao).findByName("John");
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при подсчете пользователей")
    void getUserCount_shouldHandleDaoExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        when(userDao.count()).thenThrow(new RuntimeException("Count failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserCount()
        );

        // Assert
        assertTrue(exception.getMessage().contains("Ошибка при подсчете пользователей"));
        verify(userDao).count();
    }

    @Test
    @DisplayName("Service: Обработка исключений при маппинге в createUser")
    void createUser_shouldHandleMapperExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");

        when(userDao.existsByEmail(anyString())).thenReturn(false);
        when(userMapper.toEntity(any())).thenThrow(new RuntimeException("Mapping failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));
        verify(userDao).existsByEmail("test@example.com");
        verify(userMapper).toEntity(any());
        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений при маппинге в updateUser")
    void updateUser_shouldHandleMapperExceptions() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        User existingUser = new User();
        existingUser.setId(1L);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("New Name");

        when(userDao.findById(anyLong())).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenReturn(updatedUser);
        when(userMapper.toResponse(any())).thenThrow(new RuntimeException("Response mapping failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось обновить пользователя"));
        verify(userDao).findById(1L);
        verify(userDao).save(any());
        verify(userMapper).toResponse(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений при проверке email в updateUser")
    void updateUser_shouldHandleExceptionWhenCheckingEmail() {
        // Arrange
        UserService userService = new UserService(userDao, userMapper);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");

        when(userDao.findById(anyLong())).thenReturn(Optional.of(existingUser));
        when(userDao.existsByEmail(anyString())).thenThrow(new RuntimeException("Email check failed"));

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        // Assert
        assertTrue(exception.getMessage().contains("Не удалось обновить пользователя"));
        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("new@example.com");
        verify(userDao, never()).save(any());
    }
}
