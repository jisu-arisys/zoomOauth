package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.entity.UserList;
import me.test.oauth.service.UserListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
/** 사용자의 요청에 REST API 응답값을 보내는 컨트롤러 **/
public class HelloController {

    @Autowired
    private final ZoomController zoomController;

    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    @Autowired
    private final UserListService userListService;

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        System.out.println("helloController");
        return ResponseEntity.ok("hello");
    }

    /** api 조회 후 DB 저장하고 리스트 dto 반환 **/
    public List<UserList> getUserList(@RequestParam String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        System.out.println(usersUrl);
        List<UserList> allUsers = new ArrayList<>();
        String next_page_token = "";

        //사용자 한명 조회
        String json = zoomController.getApi(usersUrl);
        if (json.startsWith("fail")){
            return null;
        }

        do{
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            String usersJson = objectMapper.writeValueAsString(parsed.get("users"));
            if (usersJson != null) {
                List<UserList> userDtos = objectMapper.readValue(usersJson, new TypeReference<List<UserList>>() {});
                allUsers.addAll(userDtos);
            }


            next_page_token = (String) parsed.get("next_page_token");
            if (next_page_token.length() > 0) {
                String nextUsersUrl = "/users?next_page_token=" + next_page_token;
                json = zoomController.getApi(nextUsersUrl);
                if (json.startsWith("fail")){
                    next_page_token = "";
                }
            }

        }while(userId.isEmpty() && next_page_token.length() > 0);

        userListService.saveAll(allUsers);

        return allUsers;
    }

    /** 한 사용자 기준 1분에 한번만 상태변경 가능. **/
    @PostMapping("/users/{userId}/presence_status")
    public ResponseEntity<String> setUserPresenceStatus(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) {
        log.info("[test]setUserPresenceStatus : {}", bodyMap);
        String changeUrl = "/users/" + userId + "/presence_status";
        String json = zoomController.postApi(changeUrl, bodyMap);
        if (json.startsWith("fail")){
            return ResponseEntity.badRequest().body(json);
        }
        return ResponseEntity.ok("success");
    }

    /** DB에서 사용자 목록 조회하고, 없으면 API 호출**/
    @GetMapping("/userlist")
    public ResponseEntity<List<UserList>> getUserlistFromDB() throws JsonProcessingException {
        List<UserList> list = userListService.findAll();

        //DB 내 데이터 없으면 API 호출하고 해당 데이터 반환.
        if(list.isEmpty()){
            list = getUserList("");
        }
        return ResponseEntity.ok(list);
    }

}
