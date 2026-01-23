package UserService.integration.controller;

import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.entity.User;
import UserService.dao.UserDao;
import UserService.kafka.UserEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.testcontainers.shaded.org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserDao userDao;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @BeforeEach
    void setUp() {
        userDao.deleteAll();
        reset(userEventProducer);
        doNothing().when(userEventProducer).sendUserCreatedEvent(anyLong(), anyString(), anyString());
        doNothing().when(userEventProducer).sendUserDeletedEvent(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Controller: POST /api/users - Успешное создание пользователя")
    void createUser_ShouldReturnCreatedStatus() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setAge(30);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.age").value(30));

        List<User> users = userDao.findAll();
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Controller: POST /api/users - Создание пользователя с невалидными данными возвращает 400")
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest();
        invalidRequest.setName("");
        invalidRequest.setEmail("invalid-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Controller: GET /api/users/{id} - Успешное получение пользователя по ID")
    void getUserById_ShouldReturnUser() throws Exception {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setAge(30);
        User savedUser = userDao.save(user);

        mockMvc.perform(get("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.age").value(30));
    }

    @Test
    @DisplayName("Controller: GET /api/users/{id} - Получение несуществующего пользователя возвращает ошибку")
    void getUserById_WithInvalidId_ShouldReturnError() throws Exception {
        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/users/{id}", 999L)));

        assertTrue(exception.getMessage().contains("Ошибка при поиске пользователя"));
    }

    @Test
    @DisplayName("Controller: GET /api/users - Успешное получение всех пользователей")
    void getAllUsers_ShouldReturnList() throws Exception {
        User user1 = new User();
        user1.setName("John Doe");
        user1.setEmail("john@example.com");
        user1.setAge(30);
        userDao.save(user1);

        User user2 = new User();
        user2.setName("Jane Doe");
        user2.setEmail("jane@example.com");
        user2.setAge(25);
        userDao.save(user2);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userResponseList.length()").value(2))
                .andExpect(jsonPath("$._embedded.userResponseList[0].name").isString())
                .andExpect(jsonPath("$._embedded.userResponseList[1].name").isString());
    }

    @Test
    @DisplayName("Controller: PUT /api/users/update/{id} - Успешное обновление пользователя")
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setAge(30);
        User savedUser = userDao.save(user);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("John Updated");
        request.setEmail("john.updated@example.com");
        request.setAge(31);

        mockMvc.perform(put("/api/users/update/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"))
                .andExpect(jsonPath("$.age").value(31));

        User updatedUser = userDao.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("John Updated");
    }

    @Test
    @DisplayName("Controller: DELETE /api/users/delete/{id} - Успешное удаление пользователя")
    void deleteUser_ShouldReturnNoContent() throws Exception {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john@example.com");
        User savedUser = userDao.save(user);

        mockMvc.perform(delete("/api/users/delete/{id}", savedUser.getId()))
                .andExpect(status().isNoContent());

        assertThat(userDao.findById(savedUser.getId())).isEmpty();
    }

    @Test
    @DisplayName("Controller: GET /api/users/search?name={name} - Успешный поиск по имени")
    void searchUsersByName_ShouldReturnMatchingUsers() throws Exception {
        User user1 = new User();
        user1.setName("John");
        user1.setEmail("john@example.com");
        userDao.save(user1);
        userDao.flush();

        mockMvc.perform(get("/api/users/search")
                        .param("name", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userResponseList[0].name").value("John"));
    }

    @Test
    @DisplayName("Controller: GET /api/users/count - Успешное получение количества пользователей")
    void getUsersCount_ShouldReturnCount() throws Exception {
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setName("User " + i);
            user.setEmail("user" + i + "@example.com");
            userDao.save(user);
        }

        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("Controller: PUT /api/users/update/{id} - Обновление несуществующего пользователя возвращает ошибку")
    void updateUser_WithInvalidId_ShouldReturnError() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("John Updated");


        Exception exception = assertThrows(Exception.class, () -> {
            mockMvc.perform(put("/api/users/update/{id}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        });


        assertTrue(exception.getMessage().contains("Request processing failed"));
    }

}