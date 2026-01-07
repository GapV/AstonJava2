package UserService.integration;

import UserService.config.TestDatabaseConfig;
import UserService.dao.UserDao;
import UserService.entity.User;
import UserService.util.TestDataFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserDaoIntegrationTest extends PostgresTestContainer {

    private UserDao userDao;
    private SessionFactory testSessionFactory;

    @BeforeEach
    void setUp() {

        testSessionFactory = TestDatabaseConfig.createTestSessionFactory();

    }

    @Test
    @Order(1)
    @DisplayName("DAO: Сохранение пользователя")
    void save_shouldSaveUserToDatabase() {

        User user = TestDataFactory.createTestUser("save_test@example.com");

        User savedUser = userDao.save(user);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals("save_test@example.com", savedUser.getEmail());
        assertNotNull(savedUser.getCreatedAt());

        Optional<User> foundUser = userDao.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @Order(2)
    @DisplayName("DAO: Поиск пользователя по ID")
    void findById_shouldReturnUserWhenExists() {

        User user = TestDataFactory.createTestUser("findbyid_test@example.com");
        User savedUser = userDao.save(user);

        Optional<User> foundUser = userDao.findById(savedUser.getId());

        assertTrue(foundUser.isPresent());
        assertEquals(savedUser.getId(), foundUser.get().getId());
        assertEquals(savedUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @Order(3)
    @DisplayName("DAO: Поиск несуществующего пользователя по ID")
    void findById_shouldReturnEmptyWhenUserNotExists() {

        Optional<User> foundUser = userDao.findById(99999L);


        assertFalse(foundUser.isPresent());
    }

    @Test
    @Order(4)
    @DisplayName("DAO: Поиск пользователя по email")
    void findByEmail_shouldReturnUserWhenEmailExists() {

        String email = "findbyemail_test@example.com";
        User user = TestDataFactory.createTestUser(email);
        userDao.save(user);


        Optional<User> foundUser = userDao.findByEmail(email);


        assertTrue(foundUser.isPresent());
        assertEquals(email, foundUser.get().getEmail());
    }

    @Test
    @Order(5)
    @DisplayName("DAO: Поиск по email должен возвращать пустой Optional если не найден")
    void findByEmail_shouldReturnEmptyWhenEmailNotExists() {

        Optional<User> foundUser = userDao.findByEmail("nonexistent@example.com");


        assertFalse(foundUser.isPresent());
    }

    @Test
    @Order(6)
    @DisplayName("DAO: Получение всех пользователей")
    void findAll_shouldReturnAllUsers() {
        List<User> existingUsers = userDao.findAll();
        existingUsers.forEach(u -> userDao.delete(u.getId()));

        User user1 = TestDataFactory.createTestUser("all1@example.com");
        User user2 = TestDataFactory.createTestUser("all2@example.com");

        userDao.save(user1);
        userDao.save(user2);

        List<User> allUsers = userDao.findAll();

        assertThat(allUsers)
                .isNotNull()
                .hasSize(2)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("all1@example.com", "all2@example.com");
    }

    @Test
    @Order(7)
    @DisplayName("DAO: Поиск пользователей по имени (частичное совпадение)")
    void findByName_shouldReturnUsersWithMatchingName() {

        User user1 = TestDataFactory.createUserWithParams("John Doe", "john1@example.com", 30);
        User user2 = TestDataFactory.createUserWithParams("John Smith", "john2@example.com", 25);
        User user3 = TestDataFactory.createUserWithParams("Jane Doe", "jane@example.com", 28);

        userDao.save(user1);
        userDao.save(user2);
        userDao.save(user3);

        List<User> johns = userDao.findByName("John");

        assertThat(johns)
                .hasSize(2)
                .extracting(User::getEmail)
                .containsExactlyInAnyOrder("john1@example.com", "john2@example.com");
    }

    @Test
    @Order(8)
    @DisplayName("DAO: Обновление пользователя")
    void update_shouldUpdateUserInDatabase() {

        User user = TestDataFactory.createTestUser("update_test@example.com");
        User savedUser = userDao.save(user);

        savedUser.setName("Updated Name");
        savedUser.setAge(30);
        User updatedUser = userDao.update(savedUser);

        assertEquals("Updated Name", updatedUser.getName());
        assertEquals(30, updatedUser.getAge());

        Optional<User> foundUser = userDao.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("Updated Name", foundUser.get().getName());
        assertEquals(30, foundUser.get().getAge());
    }

    @Test
    @Order(9)
    @DisplayName("DAO: Удаление пользователя")
    void delete_shouldRemoveUserFromDatabase() {

        User user = TestDataFactory.createTestUser("delete_test@example.com");
        User savedUser = userDao.save(user);

        assertTrue(userDao.findById(savedUser.getId()).isPresent());

        userDao.delete(savedUser.getId());

        assertFalse(userDao.findById(savedUser.getId()).isPresent());
    }

    @Test
    @Order(10)
    @DisplayName("DAO: Подсчет количества пользователей")
    void count_shouldReturnCorrectNumberOfUsers() {

        userDao.findAll().forEach(u -> userDao.delete(u.getId()));

        User user1 = TestDataFactory.createTestUser("count1@example.com");
        User user2 = TestDataFactory.createTestUser("count2@example.com");

        userDao.save(user1);
        userDao.save(user2);

        long count = userDao.count();

        assertEquals(2, count);
    }

    @Test
    @Order(11)
    @DisplayName("DAO: Проверка существования email")
    void existsByEmail_shouldReturnTrueWhenEmailExists() {

        String email = "exists_test@example.com";
        User user = TestDataFactory.createTestUser(email);
        userDao.save(user);

        assertTrue(userDao.existsByEmail(email));
        assertFalse(userDao.existsByEmail("nonexistent@example.com"));
    }

    @Test
    @Order(13)
    @DisplayName("DAO: Уникальность email - нельзя сохранить двух пользователей с одинаковым email")
    void save_shouldThrowExceptionWhenEmailAlreadyExists() {
        String email = "duplicate@example.com";
        User user1 = TestDataFactory.createTestUser(email);
        userDao.save(user1);

        User user2 = TestDataFactory.createTestUser(email);

        assertThrows(Exception.class, () -> {
            userDao.save(user2);
        });
    }

    @Test
    @Order(14)
    @DisplayName("DAO: Транзакционность - откат при ошибке")
    void transaction_shouldRollbackOnException() {

        long initialCount = userDao.count();

        try {
            // Попытка сохранить пользователя с null email (должна упасть)
            User invalidUser = new User();
            invalidUser.setName("Invalid");
            // email = null -> должна быть ошибка при commit

            userDao.save(invalidUser);

            long finalCount = userDao.count();
            assertEquals(initialCount, finalCount,
                    "Количество пользователей не должно измениться при ошибке");

        } catch (Exception e) {

            long finalCount = userDao.count();
            assertEquals(initialCount, finalCount,
                    "Количество пользователей не должно измениться при ошибке");
        }
    }
}