package me.test.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.dto.UserDetailDto;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomUser;
import me.test.oauth.entity.webhook.WebhookEvent;
import me.test.oauth.repository.ZoomUserRepository;
import me.test.oauth.repository.UserRepository;
import me.test.oauth.repository.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

import static me.test.oauth.dto.UserDetailDto.getAllUsers;

@Slf4j
@RequiredArgsConstructor
@Service
/**DB와 API 데이터의 통합조회하고(DB read > get api > DB save), DTO반환**/
public class DataService {

    @Autowired
    private ZoomApiService zoomApiService;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final ZoomUserRepository zoomUserRepository;
    @Autowired
    private final WebhookEventRepository webhookEventRepository;
    @Autowired
    private final WebSocketService webSocketService;
    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    //// api 우선조회

    /** api 조회 후 DB 저장하고 리스트 dto 반환 **/
    @Deprecated
    public List<ZoomUser> getZoomUserAll1(String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        List<ZoomUser> allUsers = new ArrayList<>();
        String nextPageToken = "";

        //사용자 한명 조회
        ResponseEntity<String> response = zoomApiService.api(usersUrl, HttpMethod.GET, null);
        if (!response.getStatusCode().equals(HttpStatusCode.valueOf(200))){
            return null;
        }
        String json = response.getBody();

        //사용자 전체 조회
        do{
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            String usersJson = objectMapper.writeValueAsString(parsed.get("users"));
            if (usersJson != null) {
                List<ZoomUser> userDtos = objectMapper.readValue(usersJson, new TypeReference<List<ZoomUser>>() {});
                allUsers.addAll(userDtos);
            }
            nextPageToken = (String) parsed.get("next_page_token");
            if (nextPageToken.length() > 0) {
                String nextUsersUrl = "/users?next_page_token=" + nextPageToken;
                json = zoomApiService.getApi(nextUsersUrl);
                if (json.startsWith("fail")){
                    nextPageToken = "";
                }
            }

        }while(userId.isEmpty() && nextPageToken.length() > 0);

        zoomUserRepository.saveAll(allUsers);

        return allUsers;
    }

    /** api 조회 후 DB 저장하고 리스트 dto 반환 v2**/
    public List<ZoomUser> getZoomUserAll(String userId) throws JsonProcessingException {
        List<ZoomUser> allUsers = new ArrayList<>();
        String url = userId.isBlank() ? "/users" : "/users/" + userId;
        String nextPageToken = "";

        do {
            ResponseEntity<String> response = zoomApiService.api(url, HttpMethod.GET, null);
            if (! response.getStatusCode().is2xxSuccessful()) break;

            Map<String, Object> parsed = objectMapper.readValue(response.getBody(), new TypeReference<>() {});

            if (parsed.get("users") == null) { //단일조회
                ZoomUser user = objectMapper.convertValue(parsed, ZoomUser.class);
                allUsers.add(user);
                break; // 단일 조회는 page 반복 없음
            }else { //users 목록 바로 매핑 (중간 JSON 문자열로 변환할 필요 없음)
                List<ZoomUser> users = objectMapper.convertValue(
                        parsed.get("users"),
                        new TypeReference<List<ZoomUser>>() {
                        }
                );
                if (users != null) {
                    allUsers.addAll(users);
                }
                //다음 페이지가 있으면 URL 변경
                nextPageToken = (String) parsed.get("next_page_token");
                if (nextPageToken != null && !nextPageToken.isBlank()) {
                    url = "/users?next_page_token=" + nextPageToken;
                }
            }

        } while (userId.isBlank() && nextPageToken != null && !nextPageToken.isBlank()); //userId 가 존재하면 단일 조회 → 루프 종료

        return allUsers;
    }

    /** api 조회 후 DB 저장하고 리스트 dto 반환 **/
    public ZoomUser getZoomUser(@RequestParam String userId) throws JsonProcessingException {
        String usersUrl = "/users/" + userId;
        log.debug("getUser : {}", usersUrl);

        //사용자 한명 조회
        String json = zoomApiService.getApi(usersUrl);
        if (json.startsWith("fail")){
            return null;
        }

        ZoomUser userDto = objectMapper.readValue(json, new TypeReference<ZoomUser>() {});
        return zoomUserRepository.save(userDto);
    }

    /** api 기준으로 db 업데이트, status 만 기존 데이터 유지 **/
    public List<ZoomUser> readZoomUserMatchGetZoomUserAndSave() throws JsonProcessingException {
        List<ZoomUser> db = zoomUserRepository.findAll();
        List<ZoomUser> api = getZoomUserAll("");
        //db 에 있지만 api 없으면 setDeleted(true);
        //db 에 없지만 api 있으면 db.add(api.get(i));
        Map<String, ZoomUser> dbMap = db.stream()
                .collect(Collectors.toMap(ZoomUser::getId, u -> u));
        Map<String, ZoomUser> apiMap = api.stream()
                .collect(Collectors.toMap(ZoomUser::getId, u -> u));

        // 결과 저장 리스트
        List<ZoomUser> result = new ArrayList<>();

        // 1) DB 에만 존재 → deleted = true
        for (ZoomUser dbUser : db) {
            if (!apiMap.containsKey(dbUser.getId())) {
                dbUser.setDeleted(true);
                log.debug("[test]  DB 에만 존재 : {}", dbUser.getEmail());
                result.add(dbUser);
            }
        }
        for (ZoomUser apiUser : api) {
            if (!dbMap.containsKey(apiUser.getId())) {
                // 2) API 에만 존재 → 신규 추가
                log.debug("[test]  API 에만 존재 : {}", apiUser.getEmail());
                //apiUser.setDeleted(false);  //기본값
                result.add(apiUser);
            }else {
                // 3) 공통 사용자 → API 정보를 기본으로 덮어쓰되, status 는 DB 기준 유지
                ZoomUser dbUser = dbMap.get(apiUser.getId());
                apiUser.setStatus(dbUser.getStatus());
                result.add(apiUser);
            }
        }
        return zoomUserRepository.saveAll(result);
    }

    //// DB 단순조회
    // zoom
    public List<ZoomUser> readAllZoomUserNotDeleted(){
        List<ZoomUser> users = zoomUserRepository.findByDeletedFalse();
        return users;
    }

    public ZoomUser readZoomUserById(String userId){
        ZoomUser user = (ZoomUser) zoomUserRepository.findByEmail(userId).orElse(null);
        return user;
    }

    public ZoomUser readZoomUserByEmail(String email) {
        ZoomUser user = (ZoomUser) zoomUserRepository.findByEmail(email).orElse(null);
        return user;
    }

    // company
    public User readUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }


    //// DB 우선조회
    /** ZoomUser & users 통합 dto 반환 **/
    public List<UserDetailDto> readZoomUserAndUser(){
        List<Object[]> result = userRepository.findAllUserWithZoomUser();
        List<UserDetailDto> findAllWithZoomUser = UserDetailDto.getAllUsers(result);
        return findAllWithZoomUser;
    }

    /** 전체 사용자 목록을 불러옴 **/
    public List<ZoomUser> readZoomUserOrGetZoomUserAndSave() throws JsonProcessingException {
        List<ZoomUser> user = readAllZoomUserNotDeleted();
        if (user.isEmpty()) {
            user = getZoomUserAll("");
        }
        return user;
    }

    /** 사용자 변경이 발생한 경우, 특정 사용자 목록을 다시 불러옴 **/
    public ZoomUser readZoomUserOrGetZoomUserAndSave(String email) throws JsonProcessingException {
        ZoomUser user = (ZoomUser) zoomUserRepository.findByEmail(email).orElse(getZoomUser(email));
        return user;
    }

    /** 사용자 정보 조회 **/
    public ZoomUser readZoomUserByIdOrGetZoomUserAndSave(String userId) throws JsonProcessingException {
        ZoomUser user = (ZoomUser) zoomUserRepository.findById(userId).orElse(getZoomUser(userId));
        return user;
    }

    //// DB 저장
    /** 검증된 webhook 이벤트를 받으면, DB에 담아 로그를 남김**/
    public WebhookEvent saveWebhook(String event, LinkedHashMap<String, Object> payloadMap, LinkedHashMap<String, Object> json) {
        log.info("saveWebhook : {} - {}", event, payloadMap);
        json.putAll(payloadMap);
        json.remove("payload");
        WebhookEvent webhookEvent = objectMapper.convertValue(json, WebhookEvent.class);
        WebhookEvent webhook = webhookEventRepository.save(webhookEvent);
        return webhook;
    }

    /** db update **/
    public boolean updateStatus(Map<String, Object>  payload){
        Map<String,Object> event = (Map<String,Object>)payload.get("object");
        String email = "";
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("event", "error");
        try {
            email = (String) event.get("email");
            String stats = (String) event.get("presence_status");
            ZoomUser userStats = (ZoomUser) zoomUserRepository.findByEmail(email).orElse(getZoomUser(email));
            userStats.setStatus(stats);
            ZoomUser result = zoomUserRepository.save(userStats);
            log.info("updateStatus success : {} - {}", result.getEmail(), result.getStatus());
            return true;

        }catch (NullPointerException e){
            log.warn("[test]updateStatus fail NullPointerException: {}", e.getMessage());
            error.put("message", String.format("findByEmail %s is NullPointerException", email));
            error.put("detail", e.getMessage());
            webSocketService.enqueueException(error);
        }catch (Exception e){
            log.warn("[test]updateStatus fail : {}", e.getMessage());
            error.put("message", String.format("maybe event data is not exist, check the event data : {}", event));
            error.put("detail", e.getMessage());
            webSocketService.enqueueException(error);
        }
        return false;
    }

    /** 사용자 삭제가 발생한 경우, 사용자 정보를 찾아서 deleted 값을 true 로 저장 **/
    public ZoomUser setUserIdDeleted(String userId) {
        Optional<ZoomUser> user = zoomUserRepository.findById(userId);
        if (user.isPresent()) {
            ZoomUser exist = user.get();
            exist.setDeleted(true);
            ZoomUser saved = zoomUserRepository.save(exist);
            return saved;
        }
        return null;
    }

    public User saveUser(User companyUser) {
        return userRepository.save(companyUser);
    }
}

