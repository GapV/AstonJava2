package UserService.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Column(name = "name",nullable = false,length = 100)
    private String name;

    @Email(message = "Некорректный формат email")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "age")
    private Integer age;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    public User(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }


}
