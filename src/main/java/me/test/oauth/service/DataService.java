package me.test.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.entity.UserList;
import me.test.oauth.entity.webhook.WebhookEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
/**DB와 API 데이터의 통합조회하고(DB 정보 없으면 api 호출 후 DB 저장), DTO반환**/
public class DataService {

    @Autowired
    private ZoomApiService zoomApiService;
    @Autowired
    private final UserListService userListService;
    @Autowired
    private final WebhookEventService webhookEventService;
    @Autowired
    private final WebSocketService webSocketService;
    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();


    /** api 조회 후 DB 저장하고 리스트 dto 반환 **/
    public List<UserList> getUserList(@RequestParam String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        System.out.println(usersUrl);
        List<UserList> allUsers = new ArrayList<>();
        String next_page_token = "";

        //사용자 한명 조회
        String json = zoomApiService.getApi(usersUrl);
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
                json = zoomApiService.getApi(nextUsersUrl);
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
        String json = zoomApiService.getApi(usersUrl);
        if (json.startsWith("fail")){
            return null;
        }

        UserList userDto = objectMapper.readValue(json, new TypeReference<UserList>() {});
        return userListService.save(userDto);
    }

    /** 사용자 변경이 발생한 경우, 특정 사용자 목록을 다시 불러옴 **/
    public boolean reloadUser(String email) throws JsonProcessingException {
        UserList user = userListService.findByEmail(email);

        //DB 내 데이터 없으면 API 호출하고 해당 데이터 반환.
        if(user == null){
            user = getUser(email);
        }

        return userListService.save(user) != null;
    }

    /** 검증된 webhook 이벤트를 받으면, DB에 담아 로그를 남김**/
    public WebhookEvent saveWebhook(String event, LinkedHashMap<String, Object> payloadMap, LinkedHashMap<String, Object> json) throws JsonProcessingException {
        log.info("[test]saveWebhook : {}\n{}", event, payloadMap);
        json.putAll(payloadMap);
        json.remove("payload");
        WebhookEvent webhookEvent = objectMapper.convertValue(json, WebhookEvent.class);
        WebhookEvent webhook = webhookEventService.saveWebhook(webhookEvent);
        return webhook;
    }

    /** db update **/
    public boolean updateStatus(Map<String, Object>  payload){
        Map<String,Object> event = (Map<String,Object>)payload.get("object");
        String email = "";
        try {
            email = (String) event.get("email");
            String stats = (String) event.get("presence_status");
            UserList result = userListService.saveStats(email, stats);
            log.info("[test]updateStatus success : {} {}", result.getId(), result.getStatus());
            return true;

        }catch (NullPointerException e){
            log.info("[test]updateStatus fail NullPointerException: {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", String.format("findByEmail %s is NullPointerException", email));
            error.put("detail", e.getMessage());
            webSocketService.enqueueException(error);
        }catch (Exception e){
            log.info("[test]updateStatus fail : {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", String.format("maybe event data is not exist, check the event data : {}", event));
            error.put("detail", e.getMessage());
            webSocketService.enqueueException(error);
        }
        return false;
    }

}
