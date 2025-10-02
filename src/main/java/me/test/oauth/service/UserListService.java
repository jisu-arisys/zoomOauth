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
}
