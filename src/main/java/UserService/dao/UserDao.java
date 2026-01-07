package UserService.dao;

import UserService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDao extends JpaRepository<User, Long> {

//    // CREATE
//    User save(User user);
//
//    // READ
//    Optional<User> findById(Long id);
//    Optional<User> findByEmail(String email);
//    List<User> findAll();
      List<User> findByName(String name);
//    List<User> findByAgeGreaterThan(int age);
//
//    // UPDATE
//    User update(User user);
//
//    // DELETE
//    void delete(Long id);
//    void delete(User user);
//
//    // Дополнительные методы
//    long count();
    boolean existsByEmail(String email);
}
