package UserService.unit.controller;

import UserService.Controller.UserController;
import UserService.dto.CreateUserRequest;
import UserService.dto.UpdateUserRequest;
import UserService.dto.UserResponse;
import UserService.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    private UserResponse testUserResponse;
    private CreateUserRequest testCreateRequest;
    private UpdateUserRequest testUpdateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        testUserResponse = new UserResponse();
        testUserResponse.setId(1L);
        testUserResponse.setName("John Doe");
        testUserResponse.setEmail("john@example.com");
        testUserResponse.setAge(30);

        testCreateRequest = new CreateUserRequest();
        testCreateRequest.setName("John Doe");
        testCreateRequest.setEmail("john@example.com");
        testCreateRequest.setAge(30);

        testUpdateRequest = new UpdateUserRequest();
        testUpdateRequest.setName("John Updated");
        testUpdateRequest.setEmail("john.updated@example.com");
        testUpdateRequest.setAge(31);
    }

    @Test
    @DisplayName("Controller: POST /api/users - Успешное создание пользователя")
    void createUser_ShouldReturnCreatedStatus() throws Exception {
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenReturn(testUserResponse);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.age").value(30));

        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
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
        when(userService.getUserById(1L)).thenReturn(testUserResponse);

        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    @DisplayName("Controller: GET /api/users/{id} - Получение несуществующего пользователя возвращает ошибку")
    void getUserById_WithInvalidId_ShouldReturnError() {
        when(userService.getUserById(999L))
                .thenThrow(new RuntimeException("Пользователь не найден"));

        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(get("/api/users/{id}", 999L)));

        assertTrue(exception.getMessage().contains("Пользователь не найден"));

        verify(userService, times(1)).getUserById(999L);
    }

    @Test
    @DisplayName("Controller: GET /api/users - Успешное получение всех пользователей")
    void getAllUsers_ShouldReturnList() throws Exception {
        List<UserResponse> users = new ArrayList<>();
        users.add(testUserResponse);

        UserResponse secondUser = new UserResponse();
        secondUser.setId(2L);
        secondUser.setName("Jane Doe");
        secondUser.setEmail("jane@example.com");
        secondUser.setAge(25);
        users.add(secondUser);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[1].id").value(2L));

        verify(userService, times(1)).getAllUsers();
    }

    @Test
    @DisplayName("Controller: PUT /api/users/update/{id} - Успешное обновление пользователя")
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        UserResponse updatedResponse = new UserResponse();
        updatedResponse.setId(1L);
        updatedResponse.setName("John Updated");
        updatedResponse.setEmail("john.updated@example.com");
        updatedResponse.setAge(31);

        when(userService.updateUser(eq(1L), any(UpdateUserRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/api/users/update/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));

        verify(userService, times(1)).updateUser(eq(1L), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("Controller: DELETE /api/users/delete/{id} - Успешное удаление пользователя")
    void deleteUser_ShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/delete/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @DisplayName("Controller: GET /api/users/search?name={name} - Успешный поиск пользователей по имени")
    void searchUsersByName_ShouldReturnMatchingUsers() throws Exception {
        List<UserResponse> searchResults = new ArrayList<>();
        searchResults.add(testUserResponse);

        when(userService.searchUsersByName("John")).thenReturn(searchResults);

        mockMvc.perform(get("/api/users/search")
                        .param("name", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("John Doe"));

        verify(userService, times(1)).searchUsersByName("John");
    }

    @Test
    @DisplayName("Controller: GET /api/users/count - Успешное получение количества пользователей")
    void getUsersCount_ShouldReturnCount() throws Exception {
        when(userService.getUserCount()).thenReturn(10L);

        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));

        verify(userService, times(1)).getUserCount();
    }

    @Test
    @DisplayName("Controller: PUT /api/users/update/{id} - Обновление несуществующего пользователя возвращает ошибку")
    void updateUser_WithInvalidId_ShouldReturnError() {
        when(userService.updateUser(eq(999L), any(UpdateUserRequest.class)))
                .thenThrow(new RuntimeException("Пользователь не найден"));

        Exception exception = assertThrows(Exception.class, () ->
                mockMvc.perform(put("/api/users/update/{id}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testUpdateRequest))));

        assertTrue(exception.getMessage().contains("Пользователь не найден"));

        verify(userService, times(1)).updateUser(eq(999L), any(UpdateUserRequest.class));
    }
}
