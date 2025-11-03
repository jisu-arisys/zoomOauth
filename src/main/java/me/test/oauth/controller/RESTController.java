package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.dto.UserDetailDto;
import me.test.oauth.entity.UserList;
import me.test.oauth.service.DataService;
import me.test.oauth.service.ZoomApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
/** 사용자의 요청에 REST 응답값을 보내는 컨트롤러 **/
public class RESTController {

    @Autowired
    private final ZoomApiService zoomApiService;

    @Autowired
    private DataService dataService;

    /** 한 사용자 기준 1분에 한번만 상태변경 요청. **/
    @PostMapping("/users/{userId}/presence_status")
    public ResponseEntity<String> setUserPresenceStatus(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) throws JsonProcessingException {
        log.info("[test]setUserPresenceStatus : {}", bodyMap);
        //api 로 존재하는 ID 체크 및 DB 정보저장
        UserList user = dataService.getUser(userId);
        if (user == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", String.format("findById %s is NullPointerException", userId));
            return ResponseEntity.badRequest().body(error.toString());
        }

        //상태변경
        String changeUrl = "/users/" + userId + "/presence_status";
        ResponseEntity<String> response = zoomApiService.api(changeUrl, HttpMethod.PUT, bodyMap);
        return response;
    }

    /** 사용자 목록 조회 **/
    @GetMapping("/userlist")
    public ResponseEntity<List<UserList>> getUserlist(@Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        List<UserList> list;
        if (isForce) {
            //api 호출 후 DB 저장
            list = dataService.readUserListMatchGetUserListAndSave();
            log.debug("[test] isForce readUserListMatchGetUserListAndSave() {}건 DB 중", list.size());
            list = dataService.readAllUserNotDeleted();
            log.debug("[test] {}건 반환", list.size());
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            log.debug("[test] readUserListOrGetUserListAndSave()");
            list = dataService.readUserListOrGetUserListAndSave();
        }
        return ResponseEntity.ok(list);
    }

    /** 사용자 변경사항 조회 **/
    @GetMapping("/user/reload/{userId}")
    public ResponseEntity<UserList> getUserReLoadFromAPI(@PathVariable String userId, @Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        UserList user;
        if (isForce) {
            //api 호출 후 DB 저장
            user = dataService.getUser(userId);
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            user = dataService.readUserOrGetUserAndSave(userId);
        }
        return ResponseEntity.ok(user);
    }

    /** 사용자 목록 변경사항 조회 **/
    @GetMapping("/user/reload/userlist")
    public ResponseEntity<List<UserList>> getUserListReLoadFromAPI(@Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        List<UserList> users;
        if (isForce) {
            //api 호출 후 DB 저장
            users = dataService.readUserListMatchGetUserListAndSave();
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            users = dataService.readUserListOrGetUserListAndSave();
        }
        return ResponseEntity.ok(users);
    }

    @PostMapping("/user/create/autoCreate")
    public ResponseEntity<String> createUserAutoCreate(@RequestBody Map<String, Object> bodyMap){
        log.info("[test]createUserAutoCreate : {}", bodyMap);
        ResponseEntity<String> json = zoomApiService.api("/users", HttpMethod.POST, bodyMap);
        log.debug("[test]createUserAutoCreate json: {}", json);
        //성공시 웹훅이벤트 받은 뒤 DB 업데이트됨.
        return json;
    }

    @DeleteMapping("/user/delete/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        log.info("[test]deleteUser : {}", userId);
        ResponseEntity<String> response = zoomApiService.api("/users/"+userId, HttpMethod.DELETE, null);
        log.debug("[test]deleteUser json: {}", response.getBody());
        //성공시 웹훅이벤트 받은 뒤 DB 업데이트됨.
        return response;
    }

    @PostMapping("/user/update/detail/{userId}")
    public ResponseEntity<String> updateUserDetail(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) {
        log.info("[test]updateUserDetail : {}", userId);
        ResponseEntity<String> response = zoomApiService.api("/users/" + userId, HttpMethod.PATCH, bodyMap);
        return response;
    }


    @GetMapping("/vue/user/list")
    public ResponseEntity<List<UserDetailDto>> getUserListVue() {
        List<UserDetailDto> list = dataService.readUserListAndUser();
        return ResponseEntity.ok(list);
    }

}
