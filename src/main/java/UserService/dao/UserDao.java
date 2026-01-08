package UserService.dao;

import UserService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserDao extends JpaRepository<User, Long> {

      Optional<User> findByEmail(String email);

      @Query("FROM User WHERE name LIKE :name")

      List<User> findByName(String name);

      boolean existsByEmail(String email);
}
