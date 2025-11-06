package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.dto.UserDetailDto;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomUser;
import me.test.oauth.service.DataService;
import me.test.oauth.service.ZoomApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

//import static me.test.oauth.entity.User.companyUserMapping;
//import static me.test.oauth.entity.ZoomUser.zoomUserMapping;

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
    @PostMapping("/zoomUser/update/presence_status/{userId}")
    public ResponseEntity<String> setUserPresenceStatus(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) throws JsonProcessingException {
        log.info("[test]setUserPresenceStatus : {}", bodyMap);
        //api 로 존재하는 ID 체크 및 DB 정보저장
        ZoomUser user = dataService.getZoomUser(userId);
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
    @GetMapping("/zoomUser")
    public ResponseEntity<List<ZoomUser>> getZoomUser(@Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        List<ZoomUser> list;
        if (isForce) {
            //api 호출 후 DB 저장
            list = dataService.readZoomUserMatchGetZoomUserAndSave();
            log.debug("[test] isForce readZoomUserMatchGetZoomUserAndSave() {}건 DB 중", list.size());
            list = dataService.readAllZoomUserNotDeleted();
            log.debug("[test] {}건 반환", list.size());
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            log.debug("[test] readZoomUserOrGetZoomUserAndSave()");
            list = dataService.readZoomUserOrGetZoomUserAndSave();
        }
        return ResponseEntity.ok(list);
    }

    /** 사용자 변경사항 조회 **/
    @GetMapping("/zoomUser/reload/{userId}")
    public ResponseEntity<ZoomUser> getUserReLoadFromAPI(@PathVariable String userId, @Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        ZoomUser user;
        if (isForce) {
            //api 호출 후 DB 저장
            user = dataService.getZoomUser(userId);
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            user = dataService.readZoomUserOrGetZoomUserAndSave(userId);
        }
        return ResponseEntity.ok(user);
    }

    /** 사용자 목록 변경사항 조회 **/
    @GetMapping("/zoomUser/reload")
    public ResponseEntity<List<ZoomUser>> getZoomUserReLoadFromAPI(@Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        List<ZoomUser> users;
        if (isForce) {
            //api 호출 후 DB 저장
            users = dataService.readZoomUserMatchGetZoomUserAndSave();
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            users = dataService.readZoomUserOrGetZoomUserAndSave();
        }
        return ResponseEntity.ok(users);
    }

    @PostMapping("/zoomUser/create/autoCreate")
    public ResponseEntity<String> createUserAutoCreate(@RequestBody Map<String, Object> bodyMap){
        log.info("[test]createUserAutoCreate : {}", bodyMap);
        ResponseEntity<String> json = zoomApiService.api("/users", HttpMethod.POST, bodyMap);
        log.debug("[test]createUserAutoCreate json: {}", json);
        //성공시 웹훅이벤트 받은 뒤 DB 업데이트됨.
        return json;
    }

    @PostMapping("/zoomUser/delete")
    public ResponseEntity<String> deleteUser(@RequestBody String id) {
        log.info("[test]deleteUser : {}", id);
        ResponseEntity<String> response = zoomApiService.api("/users/"+id, HttpMethod.DELETE, null);
        log.debug("[test]deleteUser json: {}", response.getBody());
        //성공시 웹훅이벤트 받은 뒤 DB 업데이트됨.
        return response;
    }

    @PostMapping("/zoomUser/update/detail/{userId}")
    public ResponseEntity<String> updateZoomUser(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) throws JsonProcessingException {
        log.debug("[test]updateUserDetail : {}", userId);
        ResponseEntity<String> response = zoomApiService.api("/users/" + userId, HttpMethod.PATCH, bodyMap);
        return response;
    }

    ////// zoomUser & user 통합 데이터 관리

    @GetMapping("/userDetail")
    public ResponseEntity<List<UserDetailDto>> getZoomUserVue() {
        List<UserDetailDto> list = dataService.readZoomUserAndUser();
        return ResponseEntity.ok(list);
    }

}
