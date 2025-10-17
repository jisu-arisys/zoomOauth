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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
/**DB와 API 데이터의 통합을 위한 컨트롤러 DB조회, 없으면 api 호출 후 DB 저장하고, DTO반환**/
public class APIController {

    @Autowired
    private ZoomController zoomController;

    @Autowired
    private final UserListService userListService;

    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

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

    /** api 조회 후 DB 저장하고 리스트 dto 반환 **/
    public UserList getUser(@RequestParam String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        System.out.println(usersUrl);

        //사용자 한명 조회
        String json = zoomController.getApi(usersUrl);
        if (json.startsWith("fail")){
            return null;
        }

        UserList userDto = objectMapper.readValue(json, new TypeReference<UserList>() {});
        return userListService.save(userDto);
    }

}
