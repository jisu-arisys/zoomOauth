package me.test.oauth.repository;

import me.test.oauth.dto.DtoUsers;
import me.test.oauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    @Query("""
    SELECT new me.test.oauth.dto.DtoUsers(u, zu)
    FROM User u
    JOIN ZoomUser zu ON u.email = zu.email """)
    List<DtoUsers> findAllUserWithZoomUser();

    Optional<User> findByUsername(String username);

    @Query("""
    SELECT new me.test.oauth.dto.DtoUsers(u, zu)
    FROM User u
    LEFT JOIN ZoomUser zu ON u.email = zu.email 
    WHERE u.email = :email
    """)
        DtoUsers findByIdUserWithZoomUser(@Param("email") String email);
}