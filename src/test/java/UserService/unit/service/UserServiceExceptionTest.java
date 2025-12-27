package UserService.unit.service;

import UserService.dao.UserDao;
import UserService.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceExceptionTest {

    @Mock
    private UserDao userDao;

    @Test
    @DisplayName("Service: Обработка исключений DAO при создании пользователя")
    void createUser_shouldHandleDaoExceptions() {

        UserService userService = new UserService(userDao);
        when(userDao.existsByEmail(anyString())).thenReturn(false);
        when(userDao.save(any())).thenThrow(new RuntimeException("Database connection failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser("Test", "test@example.com", 25)
        );

        assertTrue(exception.getMessage().contains("Database connection failed"));
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при получении пользователя")
    void getUserById_shouldHandleDaoExceptions() {

        UserService userService = new UserService(userDao);
        when(userDao.findById(any())).thenThrow(new RuntimeException("Query failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(1L)
        );

        assertTrue(exception.getMessage().contains("Query failed"));
    }
}
