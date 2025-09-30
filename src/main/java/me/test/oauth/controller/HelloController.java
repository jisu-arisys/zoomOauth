package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import me.test.oauth.common.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
/** 사용자의 요청에 REST API 응답값을 보내는 컨트롤러 **/
public class HelloController {

    @Autowired
    private final ZoomController zoomController;

    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        System.out.println("helloController");
        return ResponseEntity.ok("hello");
    }

    @GetMapping("/users")
    public ResponseEntity<String> getUsers(@RequestParam String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        System.out.println(usersUrl);
        List<Map<String, Object>> allUsers = new ArrayList<>();
        String next_page_token = "";

        //사용자 한명 조회
        String json = zoomController.getApi(usersUrl);
        if (json.startsWith("fail")){
            return ResponseEntity.badRequest().body(json);
        }

        do{
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> users = (List<Map<String, Object>>) parsed.get("users");
            if (users != null) {
                allUsers.addAll(users);
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


        // 응답 바디 JSON 형로 출력 : 4개의 공백으로 들여쓰기
        String prettyJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allUsers);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(prettyJsonString);
    }
}
