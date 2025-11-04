package me.test.oauth.repository;

import me.test.oauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    @Query("""
    SELECT u, ul
    FROM User u
    JOIN UserList ul ON u.email = ul.email """)
    List<Object[]> findAllWithUserList();

    Optional<User> findByUsername(String username);
}