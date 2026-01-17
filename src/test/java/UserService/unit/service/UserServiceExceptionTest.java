package UserService.unit.service;

import UserService.dao.UserDao;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.entity.User;
import UserService.kafka.UserEventProducer;
import UserService.mapper.UserMapper;
import UserService.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

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

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Service: Обработка исключений при создании пользователя - дубликат email")
    void createUser_shouldHandleDuplicateEmailException() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        User mockUser = new User("Test", "test@example.com", 25);

        when(userMapper.toEntity(request)).thenReturn(mockUser);


        DataIntegrityViolationException dive = new DataIntegrityViolationException(
                "ERROR: 23505: duplicate key value violates unique constraint"
        );
        when(userDao.save(mockUser)).thenThrow(dive);


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(request)
        );

        assertEquals(409, exception.getStatusCode().value()); // CONFLICT
        assertEquals("Такой емайл уже есть", exception.getReason());

        verify(userMapper).toEntity(request);
        verify(userDao).save(mockUser);
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при создании пользователя")
    void createUser_shouldHandleDaoExceptions() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        User mockUser = new User("Test", "test@example.com", 25);

        when(userMapper.toEntity(request)).thenReturn(mockUser);
        when(userDao.save(mockUser)).thenThrow(new RuntimeException("Database connection failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));

        verify(userMapper).toEntity(request);
        verify(userDao).save(mockUser);
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при получении пользователя")
    void getUserById_shouldHandleDaoExceptions() {

        when(userDao.findById(anyLong())).thenThrow(new RuntimeException("Query failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserById(1L)
        );

        assertTrue(exception.getMessage().contains("Ошибка при поиске пользователя"));
        verify(userDao).findById(1L);
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при получении всех пользователей")
    void getAllUsers_shouldHandleDaoExceptions() {

        when(userDao.findAll()).thenThrow(new RuntimeException("Connection error"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getAllUsers()
        );

        assertTrue(exception.getMessage().contains("Ошибка при получении пользователей"));
        verify(userDao).findAll();
    }

    @Test
    @DisplayName("Service: Обработка исключений при обновлении пользователя - email уже занят")
    void updateUser_shouldHandleDuplicateEmailException() {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByEmail("new@example.com")).thenReturn(true);


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, request)
        );

        assertEquals("Новый email уже занят", exception.getMessage());

        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("new@example.com");
        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при обновлении пользователя")
    void updateUser_shouldHandleDaoSaveExceptions() {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any())).thenThrow(new RuntimeException("Update failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        assertEquals("Update failed", exception.getMessage());
        verify(userDao).findById(1L);
        verify(userDao).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при удалении пользователя")
    void deleteUser_shouldHandleDaoExceptions() {

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setName("Test User");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        doThrow(new RuntimeException("Delete failed")).when(userDao).deleteById(1L);


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.deleteUser(1L)
        );

        assertEquals("Delete failed", exception.getMessage());
        verify(userDao).findById(1L);
        verify(userDao).deleteById(1L);
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при поиске по имени")
    void searchUsersByName_shouldHandleDaoExceptions() {

        when(userDao.findByName("John")).thenThrow(new RuntimeException("Search failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.searchUsersByName("John")
        );

        assertTrue(exception.getMessage().contains("Ошибка при поиске пользователей"));
        verify(userDao).findByName("John");
    }

    @Test
    @DisplayName("Service: Обработка исключений DAO при подсчете пользователей")
    void getUserCount_shouldHandleDaoExceptions() {

        when(userDao.count()).thenThrow(new RuntimeException("Count failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.getUserCount()
        );

        assertTrue(exception.getMessage().contains("Ошибка при подсчете пользователей"));
        verify(userDao).count();
    }

    @Test
    @DisplayName("Service: Обработка исключений при маппинге в createUser")
    void createUser_shouldHandleMapperExceptions() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        when(userMapper.toEntity(request)).thenThrow(new RuntimeException("Mapping failed"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));
        verify(userMapper).toEntity(request);
        verify(userDao, never()).save(any());
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Service: Обработка исключений при проверке email в updateUser")
    void updateUser_shouldHandleExceptionWhenCheckingEmail() {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("old@example.com");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.existsByEmail("new@example.com")).thenThrow(new RuntimeException("Email check failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        assertEquals("Email check failed", exception.getMessage());
        verify(userDao).findById(1L);
        verify(userDao).existsByEmail("new@example.com");
        verify(userDao, never()).save(any());
    }

    @Test
    @DisplayName("Service: Обработка исключений при маппинге в updateUser")
    void updateUser_shouldHandleResponseMapperExceptions() {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New Name");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setName("New Name");
        updatedUser.setEmail("test@example.com");

        when(userDao.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userDao.save(any(User.class))).thenReturn(updatedUser);
        when(userMapper.toResponse(updatedUser)).thenThrow(new RuntimeException("Response mapping failed"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateUser(1L, request)
        );

        assertEquals("Response mapping failed", exception.getMessage());
        verify(userDao).findById(1L);
        verify(userDao).save(any(User.class));
        verify(userMapper).toResponse(updatedUser);
    }


    @Test
    @DisplayName("Service: Обработка общего исключения в createUser")
    void createUser_shouldHandleGeneralException() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        User mockUser = new User("Test", "test@example.com", 25);

        when(userMapper.toEntity(request)).thenReturn(mockUser);


        when(userDao.save(mockUser)).thenThrow(new RuntimeException("Some other error"));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));

        verify(userMapper).toEntity(request);
        verify(userDao).save(mockUser);
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Service: createUser - исключение при маппинге и DataIntegrityViolationException")
    void createUser_shouldHandleMapperExceptionWithDataIntegrity() {

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test");
        request.setEmail("test@example.com");
        request.setAge(25);

        when(userMapper.toEntity(request)).thenThrow(new RuntimeException("Mapping error"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.createUser(request)
        );

        assertTrue(exception.getMessage().contains("Не удалось сохранить пользователя"));

        verify(userMapper).toEntity(request);
        verify(userDao, never()).save(any());
        verify(userEventProducer, never()).sendUserCreatedEvent(anyLong(), anyString(), anyString());
    }
}
