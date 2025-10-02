package me.test.oauth.repository;

import me.test.oauth.entity.UserList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserListRepository extends JpaRepository<UserList, String> {}