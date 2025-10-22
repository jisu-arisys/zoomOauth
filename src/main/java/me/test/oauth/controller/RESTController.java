package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.entity.UserList;
import me.test.oauth.service.DataService;
import me.test.oauth.service.UserListService;
import me.test.oauth.service.ZoomApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
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
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    @Autowired
    private final UserListService userListService;

    @Autowired
    private DataService dataService;

    /** 한 사용자 기준 1분에 한번만 상태변경 요청. **/
    @PostMapping("/users/{userId}/presence_status")
    public ResponseEntity<String> setUserPresenceStatus(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) {
        log.info("[test]setUserPresenceStatus : {}", bodyMap);
        Optional<UserList> user = userListService.findById(userId);
        if (user.isEmpty()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", String.format("findById %s is NullPointerException", userId));
            return ResponseEntity.badRequest().body(error.toString());
        }

        String changeUrl = "/users/" + userId + "/presence_status";
        String json = zoomApiService.postApi(changeUrl, bodyMap);
        if (json.startsWith("fail")){
            return ResponseEntity.badRequest().body(json);
        }
        return ResponseEntity.ok("success");
    }

    /** 사용자 목록 조회 **/
    @GetMapping("/userlist")
    public ResponseEntity<List<UserList>> getUserlistFromDB() throws JsonProcessingException {
        List<UserList> list = userListService.findAll();

        //DB 내 데이터 없으면 API 호출하고 해당 데이터 반환.
        if(list.isEmpty()){
            list = dataService.getUserList("");
        }
        return ResponseEntity.ok(list);
    }

    /** 사용자 변경사항 조회 **/
    @GetMapping("/user/reload/{userId}")
    public ResponseEntity<UserList> getUserReLoadFromAPI(@PathVariable String userId, @Param(value = "false") String force) throws JsonProcessingException {
        boolean isForce = Boolean.parseBoolean(force);
        UserList user = userListService.findByEmail(userId);
        if (isForce || user == null) {
            user = dataService.getUser(userId);
        }
        return ResponseEntity.ok(user);
    }

}
