package me.test.oauth.repository;

import me.test.oauth.entity.ZoomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoomUserRepository extends JpaRepository<ZoomUser, String> {
    Optional<Object> findByEmail(String email);

    List<ZoomUser> findByDeletedFalse();
}