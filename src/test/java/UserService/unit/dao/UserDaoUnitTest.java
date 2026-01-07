package UserService.unit.dao;

import UserService.dao.UserDao;

import UserService.entity.User;
import UserService.util.TestDataFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDaoUnitTest {

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private Transaction transaction;

    @Mock
    private Query<User> query;

    @Mock
    private Query<Long> longQuery;
    @Mock
    private UserDao userDao;


    @BeforeEach
    void setUp() {

    }
    @AfterEach
    void tearDown() {

    }
    @Test
    @DisplayName("DAO Unit: Поиск по ID - успешный")
    void findById_shouldReturnUserWhenExists() {

        User expectedUser = TestDataFactory.createTestUser(1L, "test@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.get(User.class, 1L)).thenReturn(expectedUser);

        Optional<User> result = userDao.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(expectedUser, result.get());
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Поиск по ID - не найден")
    void findById_shouldReturnEmptyWhenNotFound() {

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.get(User.class, 999L)).thenReturn(null);

        Optional<User> result = userDao.findById(999L);

        assertFalse(result.isPresent());
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Поиск по email")
    void findByEmail_shouldReturnUserWhenEmailExists() {

        User expectedUser = TestDataFactory.createTestUser(1L, "test@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.setParameter("email", "test@example.com")).thenReturn(query);
        when(query.uniqueResultOptional()).thenReturn(Optional.of(expectedUser));

        Optional<User> result = userDao.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(expectedUser, result.get());
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Получение всех пользователей")
    void findAll_shouldReturnAllUsers() {

        List<User> expectedUsers = Arrays.asList(
                TestDataFactory.createTestUser(1L, "user1@example.com"),
                TestDataFactory.createTestUser(2L, "user2@example.com")
        );

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.createQuery(anyString(), eq(User.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedUsers);

        List<User> result = userDao.findAll();

        assertEquals(2, result.size());
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Сохранение пользователя")
    void save_shouldSaveUser() {

        User userToSave = TestDataFactory.createTestUser("new@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        userDao.save(userToSave);

        verify(session).persist(userToSave);
        verify(transaction).commit();
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Сохранение пользователя - откат при ошибке")
    void save_shouldRollbackOnException() {

        User userToSave = TestDataFactory.createTestUser("new@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        doThrow(new RuntimeException("DB error")).when(session).persist(userToSave);

        assertThrows(RuntimeException.class, () -> userDao.save(userToSave));

        verify(transaction).rollback();
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Обновление пользователя")
    void update_shouldUpdateUser() {

        User userToUpdate = TestDataFactory.createTestUser(1L, "updated@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.merge(userToUpdate)).thenReturn(userToUpdate);

        User result = userDao.update(userToUpdate);

        assertEquals(userToUpdate, result);
        verify(transaction).commit();
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Удаление пользователя")
    void delete_shouldDeleteUser() {

        User userToDelete = TestDataFactory.createTestUser(1L, "delete@example.com");

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.get(User.class, 1L)).thenReturn(userToDelete);

        userDao.delete(1L);

        verify(session).remove(userToDelete);
        verify(transaction).commit();
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Удаление несуществующего пользователя")
    void delete_shouldNotThrowWhenUserNotFound() {

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        when(session.get(User.class, 999L)).thenReturn(null);

        userDao.delete(999L);

        verify(session, never()).remove(any());
        verify(transaction).commit();
        verify(session).close();
    }

    @Test
    @DisplayName("DAO Unit: Подсчет пользователей")
    void count_shouldReturnCount() {

        when(sessionFactory.openSession()).thenReturn(session);

        when(session.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
        when(longQuery.uniqueResult()).thenReturn(5L);

        long count = userDao.count();

        assertEquals(5L, count);
        verify(session).createQuery("SELECT COUNT(*) FROM User", Long.class);
        verify(longQuery).uniqueResult();
        verify(session).close();
    }
}
