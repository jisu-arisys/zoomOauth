package me.test.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.dto.DtoOrganization;
import me.test.oauth.dto.DtoUsers;
import me.test.oauth.entity.*;
import me.test.oauth.entity.webhook.WebhookEvent;
import me.test.oauth.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
/** 기능별로 내부클래스로 관리. 클래스별로 api 우선, db 우선, db 온리, 비즈니스 로직추가 등 차별화 **/
public class DataService {

    @Autowired
    private ZoomApiService zoomApiService;
    @Autowired
    private final DeptRepository deptRepository;
    @Autowired
    private final AuthRepository authRepository;
    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final ZoomLicenseRepository zoomLicenseRepository;
    @Autowired
    private final ZoomUserRepository zoomUserRepository;
    @Autowired
    private final WebhookEventRepository webhookEventRepository;
    @Autowired
    private final WebSocketService webSocketService;
    @Autowired
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    public WhookOps whookOps = new WhookOps();
    public ZoomOps zoomOps = new ZoomOps();
    public OrgOps orgOps = new OrgOps();

    //// DB 단순 조회, 저장
    // zoom
    public List<ZoomUser> readAllZoomUserNotDeleted(){
        List<ZoomUser> users = zoomUserRepository.findByDeletedFalse();
        return users;
    }

    public ZoomUser getZoomUserById(String userId){
        ZoomUser user = zoomUserRepository.findByEmail(userId).orElse(null);
        return user;
    }

    public ZoomUser getZoomUserByEmail(String email) {
        ZoomUser user = zoomUserRepository.findByEmail(email).orElse(null);
        return user;
    }

    public ZoomLicense getLicenseByName(String licenseName) {
        return zoomLicenseRepository.findTypeByName(licenseName);
    }

    public List<ZoomLicense> getLicense() {
        return zoomLicenseRepository.findAll();
    }

    // user & zoomUser 통합
    /** zoom 계정이 있는 직원만 조회 가능 **/
    public List<DtoUsers> getUserAndZoomUsersByEmails(List<String> emails){
        return userRepository.findAllUserWithZoomUserByEmail(emails);
    }
    public DtoUsers getZoomUserAndUserByEmail(String email){
        return userRepository.findByIdUserWithZoomUser(email);
    }
    public List<DtoUsers> getZoomUserAndUser(){
        return userRepository.findAllUserWithZoomUser();
    }

    /** user only 직원도 조회 가능 **/
    public List<DtoUsers> getDtoUsersByEmails(List<String> emails){
        return userRepository.findAllDtoUserByEmail(emails);
    }
    public List<DtoUsers> getDtoUsersByEmails(){
        return userRepository.findAllDtoUserByEmail();
    }

    // company
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> saveAllUser(List<User> users) {
        return userRepository.saveAll(users);
    }

    public User saveUser(User companyUser) {
        return userRepository.save(companyUser);
    }

    public List<Authority> getAuthorities() {
        return authRepository.findAll();
    }

    public List<Department> saveAllDept(List<Department> depts) {
        return deptRepository.saveAll(depts);
    }

    public List<Department> getDepartments() {
        return deptRepository.findAll();
    }

    //// Transactional 관리 - 데이터 일관성(Consistency) 과 성능 최적화
    @Transactional(readOnly = true) //수정 금지 + 성능 최적화
    public Map<String, List<?>> loadBaseOptions() {
        return Map.of(
                "dept", getDepartments(),
                "userType", getAuthorities(),
                "license", getLicense()
        );
    }


    //// zoom api 호출
    public class ZoomOps {
        //// api 우선조회
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
            ResponseEntity<String> response = zoomApiService.api(usersUrl, HttpMethod.GET, null);
            if (! response.getStatusCode().is2xxSuccessful()) return null;
            String json = response.getBody();
            log.debug("getUser json : {}", json);

            ZoomUser userDto = objectMapper.readValue(json, new TypeReference<ZoomUser>() {});
            //영속성을 위해 ZoomLicense 조회 후 다시 담아주기
            ZoomLicense exist = zoomLicenseRepository.findByType(userDto.getType()).orElse(null);
            userDto.setLicenseInfoList(exist);
            return zoomUserRepository.save(userDto);
        }

        /** api 기준으로 db 업데이트, status 만 기존 데이터 유지 **/
        public List<ZoomUser> patchByGetStatus() throws JsonProcessingException {
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

        //// DB 우선조회

        /** 전체 사용자 목록을 불러옴 **/
        public List<ZoomUser> readOrGetAndSaveZoomUser() throws JsonProcessingException {
            List<ZoomUser> user = readAllZoomUserNotDeleted();
            if (user.isEmpty()) {
                user = getZoomUserAll("");
            }
            return user;
        }

        /** email 로 DB 우선조회, 없으면 apI 로 특정 사용자 정보를 다시 불러옴 **/
        public ZoomUser readOrGetAndSaveZoomUserByEmail(String email) throws JsonProcessingException {
            return zoomUserRepository.findByEmail(email).orElse(getZoomUser(email));
        }

        /** id 로 DB 우선조회, 없으면 apI 로 특정 사용자 정보를 다시 불러옴 **/
        public ZoomUser readOrGetAndSaveZoomUserById(String userId) throws JsonProcessingException {
            return zoomUserRepository.findById(userId).orElse(getZoomUser(userId));
        }
    }

    //// webhook DB 저장
    public class WhookOps {
        /** webhook 수신 후, DB에 담아 로그를 남김**/
        public WebhookEvent saveWebhook(String event, LinkedHashMap<String, Object> payloadMap, LinkedHashMap<String, Object> json) {
            //log.info("saveWebhook : {} - {}", event, payloadMap);
            json.putAll(payloadMap);
            json.remove("payload");
            WebhookEvent webhookEvent = objectMapper.convertValue(json, WebhookEvent.class);
            WebhookEvent webhook = webhookEventRepository.save(webhookEvent);
            return webhook;
        }

        /** webhook 수신 후 zoomUser 현재상태 db update **/
        public boolean updateStatus(Map<String, Object>  payload){
            Map<String,Object> event = (Map<String,Object>)payload.get("object");
            String email = "";
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            try {
                email = (String) event.get("email");
                String stats = (String) event.get("presence_status");
                ZoomUser userStats = (ZoomUser) zoomUserRepository.findByEmail(email).orElse(zoomOps.getZoomUser(email));
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

        /** webhook 수신 후 삭제된 사용자 정보를 찾아서 deleted 값을 true 로 저장 (논리적삭제) **/
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
    }

    //// DtoUsers 통합 관리
    public class OrgOps {
        /* User, ZoomUser, api 호출용 map
         * merge - db 에서 변경항목만 수정된 객체 반환
         * patch - merge 한 객체를 가져와 db 및 api 에 반영
         * */

        // dept + user = organization

        /** 조직정보 조회 **/
        public List<DtoOrganization> getOrg() {
            return deptRepository.findAllByDeptParentCode();
        }
        /** 조직정보 및 소속 사용자 조회 **/
        public List<DtoOrganization> getOrgJoinUser() {
            return deptRepository.findAllByDeptParentCodeByUser();
        }

        // 새로 조회한 List<DtoUsers> 에서 List<User> 만 병합하여 반환함.
//    public List<DtoUsers> mergeDtoUsersUserByUsers(List<User> users){
//        List<String> emails = users.stream().map(User::getEmail).collect(Collectors.toList());
//        List<DtoUsers> dbDtoUsers = getUserAndZoomUsersByEmails(emails);
//        for (DtoUsers db : dbDtoUsers) {
//            String email = db.getUser().getEmail();
//            User user = users.stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
//            if (user != null) {
//                db.setUser(user);
//            }
//        }
//        return dbDtoUsers;
//    }

        //List<DtoUsers> 에서 변경된 필드만 업데이트한 병합된 List<DtoUsers> 를 반환. 업데이트 목록과 db목록이 다르면 메일주소로 새로 조회.
        public List<DtoUsers> patchByDtoUser(List<DtoUsers> updateDtoUsers, List<DtoUsers> dbDtoUsers){
            //비교를 위한 DB 목록 조회
            List<String> emails = updateDtoUsers.stream().map(DtoUsers::getUser).map(User::getEmail).collect(Collectors.toList());
            if (dbDtoUsers == null || updateDtoUsers.size() != dbDtoUsers.size()) dbDtoUsers = getUserAndZoomUsersByEmails(emails);

            //병합, api 호출
            List<User> mergedUsers = new ArrayList<>();
            for (DtoUsers db : dbDtoUsers) {
                String email = db.getUser().getEmail();
                DtoUsers up = updateDtoUsers.stream().filter(u -> u.getUser().getEmail().equals(email)).findFirst().orElse(null);
                if (up != null) {
                    //객체별, 필드별 merge 함수 실행. api 호출
                    Map <String, Object> zoomMap = new HashMap<>();
                    User mergedUser = mergeByDtoUser(up,db,zoomMap).getUser(); //db.set
                    mergedUsers.add(mergedUser);
                    zoomApiService.api("/users/" + email, HttpMethod.PATCH, zoomMap);
                }
            }

            //user 일괄저장
            List<User> savedUsers = userRepository.saveAll(mergedUsers);

            return dbDtoUsers;
        }

        //List<User> 에서 변경된 필드만 업데이트한 병합된 List<DtoUsers> 를 반환. 업데이트 목록과 db목록이 다르면 메일주소로 새로 조회.
        // user only 직원은 DtoUsers 조회 불가로 업데이트 안되는 문제점. 존재하지 않는 id에 대해 zoom api 호출됨 에러.
        public List<DtoUsers> patchByUser(List<User> updateUsers, List<DtoUsers> dbDtoUsers){
            //비교를 위한 DB 목록 조회
            List<String> emails = updateUsers.stream().map(User::getEmail).collect(Collectors.toList());
            if (dbDtoUsers == null || updateUsers.size() != dbDtoUsers.size()) dbDtoUsers = getDtoUsersByEmails(emails); //user only 직원도 조회하기

            //병합, api 호출
            List<User> mergedUsers = new ArrayList<>();
            for (DtoUsers db : dbDtoUsers) {
                String email = db.getUser().getEmail();
                User up = updateUsers.stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
                if (up != null) {
                    //객체별, 필드별 merge 함수 실행. api 호출
                    Map <String, Object> zoomMap = new HashMap<>();
                    User mergedUser = mergeByUser(up,db,zoomMap).getUser(); //db.set , userOnly check
                    mergedUsers.add(mergedUser);
                    if(db.getZoomUser() != null){ //user only 직원은 api 호출 안함.
                        zoomApiService.api("/users/" + email, HttpMethod.PATCH, zoomMap);
                    }
                }
            }

            //user 일괄저장
            List<User> savedUsers = userRepository.saveAll(mergedUsers);

            return dbDtoUsers;
        }

        // DtoUsers 에서 변경된 필드 추적하여, 병합된 DtoUser와 변경사항 추가된 zoomMaps 를 반환.
        public DtoUsers mergeByDtoUser(DtoUsers updateDtoUser, DtoUsers db, Map<String, Object> zoomMap){
            if (db == null) db = userRepository.findByIdUserWithZoomUser(updateDtoUser.getUser().getEmail());
            if (db == null) return updateDtoUser;
            if (zoomMap == null) zoomMap = new HashMap<>();

            User upUser = db.getUser();
            ZoomUser upZoomUser = db.getZoomUser();
            User dbUser = db.getUser();
            ZoomUser dbZoomUser = db.getZoomUser();

            // user.username
            String username = upUser.getUsername();
            if(!dbUser.getUsername().equals(username) || !dbZoomUser.getDisplayName().equals(username)){
                dbUser.setUsername(username);
                dbZoomUser.setDisplayName(username);
                zoomMap.put("displayName", username);
            }

            // user.dept
            Department department = upUser.getDept();
            String deptName = department.getDeptName();
            if(!dbUser.getDept().getDeptCode().equals(department.getDeptCode()) || !dbZoomUser.getDept().equals(deptName)){
                dbUser.setDept(department);
                dbZoomUser.setDept(deptName);
                zoomMap.put("dept", deptName);
            }

            // user.positon
            String positon = upUser.getPosition();
            if(!dbUser.getPosition().equals(positon) || !dbZoomUser.getJobTitle().equals(positon)){
                dbUser.setPosition(positon);
                dbZoomUser.setJobTitle(positon);
                zoomMap.put("jopTitle", positon);
            }

            // zoomUser.licenseInfoList
            ZoomLicense newLic = upZoomUser.getLicenseInfoList();
            if(!dbZoomUser.getLicenseInfoList().getType().equals(newLic)){
                dbZoomUser.setLicenseInfoList(newLic);
                zoomMap.put("type", String.valueOf(newLic.getType()));
            }

            return new DtoUsers(dbUser, dbZoomUser);
        }

        // User 에서 변경된 필드 추적하여, 병합된 DtoUser와 변경사항 추가된 zoomMaps 를 반환. user only 직원 체크
        public DtoUsers mergeByUser(User upUser, DtoUsers db, Map<String, Object> zoomMap){
            if (db == null) db = userRepository.findByIdUserWithZoomUser(upUser.getEmail());
            if (db == null) return null;
            if (zoomMap == null) zoomMap = new HashMap<>();

            User dbUser = db.getUser();
            ZoomUser dbZoomUser = db.getZoomUser();
            boolean isUserOnly = dbZoomUser == null;

            // user.username
            String username = upUser.getUsername();
            if(!dbUser.getUsername().equals(username)){
                dbUser.setUsername(username);
                if(!isUserOnly && !dbZoomUser.getDisplayName().equals(username)){
                    dbZoomUser.setDisplayName(username);
                    zoomMap.put("displayName", username);
                }
            }

            // user.dept
            Department department = upUser.getDept();
            String deptName = department.getDeptName();
            if(!dbUser.getDept().getDeptCode().equals(department.getDeptCode())){
                dbUser.setDept(department);
                if(!isUserOnly && !dbZoomUser.getDept().equals(deptName)){
                    dbZoomUser.setDept(deptName);
                    zoomMap.put("dept", deptName);
                }
            }

            // user.positon
            String positon = upUser.getPosition();
            if(!dbUser.getPosition().equals(positon)){
                dbUser.setPosition(positon);
                if(!isUserOnly && !dbZoomUser.getJobTitle().equals(positon)){
                    dbZoomUser.setJobTitle(positon);
                    zoomMap.put("jopTitle", positon);
                }
            }

            return new DtoUsers(dbUser, dbZoomUser);
        }

        //List<DtoUsers> 에서 부서정보만 업데이트한 병합된 List<DtoUsers> 를 반환. 업데이트 목록과 db목록이 다르면 메일주소로 새로 조회.
//    public List<DtoUsers> mergeDtoUsersDeptByUsers(List<User> updateUsers, List<DtoUsers> db){
//        List<String> emails = updateUsers.stream().map(User::getEmail).collect(Collectors.toList());
//        if (db == null || updateUsers.size() != db.size()) db = getUserAndZoomUsersByEmails(emails);
//
//        for (DtoUsers dto : db) {
//            ZoomUser dbZoomUser = dto.getZoomUser();
//            User dbUser = dto.getUser();
//            User updateUser = updateUsers.stream().filter(u -> u.getEmail().equals(dbUser.getEmail())).findFirst().orElse(null);
//            if (updateUser != null) {
//                dbUser.setDept(updateUser.getDept()); //부서정보
//                dbUser.setIndex(updateUser.getIndex()); //정렬순서
//                dbZoomUser.setDept(updateUser.getDept().getDeptName()); //줌 부서이름
//            }
//        }
//
//        return db;
//    }

        //List<DtoUsers> 에서 사용자이름만 업데이트한 List<DtoUsers> 를 반환. zoom 변경사항도 반환. 업데이트 목록과 db목록이 다르면 메일주소로 새로 조회.
//    public List<DtoUsers> mergeDtoUsersUserNameByUsers(List<User> updateUsers, List<DtoUsers> db, Map<String,Map<String, Object>> zoomMaps){
//        List<String> emails = updateUsers.stream().map(User::getEmail).collect(Collectors.toList());
//        if (db == null || updateUsers.size() != db.size()) db = getUserAndZoomUsersByEmails(emails);
//        if (zoomMaps == null) zoomMaps = new HashMap<>();
//
//        for (DtoUsers dto : db) {
//            ZoomUser dbZoomUser = dto.getZoomUser();
//            User dbUser = dto.getUser();
//            User updateUser = updateUsers.stream().filter(u -> u.getEmail().equals(dbUser.getEmail())).findFirst().orElse(null);
//            if (updateUser != null) {
//                String updatedUserName = updateUser.getUsername(); // 사용자 이름
//                dbUser.setUsername(updatedUserName);
//                dbZoomUser.setDisplayName(updatedUserName);
//                zoomMaps.put(updateUser.getEmail(), Map.of("displayName", updatedUserName)); // {{email: {displayName: updatedUserName}},...}
//            }
//        }
//
//        return db;
//    }

        // 부서정보 변경만 api 호출함. db 업데이트는 webhook 수신 후 반영됨. 임시 변경내용 반환.
        public List<ZoomUser> saveAllZoomUserByDeptCode(List<ZoomUser> zoomUsers) {
            zoomUsers.forEach(user -> {
                String id = user.getEmail();
                String changedDeptName = user.getDept();
                Map<String, Object> zoomMap = new HashMap<>();
                zoomMap.put("dept", changedDeptName);
                zoomApiService.api("/users/" + id, HttpMethod.PATCH, zoomMap);
            });
            return zoomUsers;
        }
    }
}

