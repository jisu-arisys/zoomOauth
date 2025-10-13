package me.test.oauth.service;

import lombok.AllArgsConstructor;
import me.test.oauth.entity.UserList;
import me.test.oauth.repository.UserListRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserListService {

    @Autowired
    private final UserListRepository repository;


    public List<UserList> findAll(){
        return repository.findAll();
    }

    public List<UserList> saveAll(List<UserList> userList){
        return repository.saveAll(userList);
    }

    /** 존재하지 않는 이메일이면 결과가 null일 수도 있음 -> NullPointerException (NPE) 발생 **/
    public UserList findByEmail(String email) {
        return (UserList) repository.findByEmail(email).orElse(null);
    }

    public UserList saveStats(String email, String stats) {
        UserList userStats = findByEmail(email);
        userStats.setStatus(stats);
        return repository.save(userStats);
    }
}
