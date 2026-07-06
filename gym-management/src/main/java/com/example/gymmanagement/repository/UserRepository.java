package com.example.gymmanagement.repository;
import com.example.gymmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    List<User> findByStatusTrue();
    @Query("SELECT u FROM User u WHERE u.role.roleName = 'ROLE_USER' AND u.status = true")
    List<User> findAllActiveUsers();
}