package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.dto.DtoUsers;
import me.test.oauth.entity.Authority;
import me.test.oauth.entity.User;
import me.test.oauth.entity.ZoomLicense;
import me.test.oauth.entity.ZoomUser;
import me.test.oauth.service.DataService;
import me.test.oauth.service.ZoomApiService;
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

    private final ZoomApiService zoomApiService;
    private final DataService dataService;
    private final ObjectMapper objectMapper;

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
    public ResponseEntity<List<DtoUsers>> getZoomUserVue() {
        List<DtoUsers> list = dataService.readZoomUserAndUser();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/userDetail/delete")
    public ResponseEntity<String> deleteZoomUserVue(@RequestBody String email) {
        log.info("[test]deleteZoomUserVue : {}", email);
        ResponseEntity<String> response = zoomApiService.api("/users/"+email, HttpMethod.DELETE, null);
        return response;
    }

    //동일사항이면 webhook 발생안함. 그냥 사용자 요청내용 다 담아서 보내면?
    @PostMapping("/userDetail/update/detail/{userId}")
    public ResponseEntity<DtoUsers> updateUserDetail(@PathVariable String userId, @RequestBody DtoUsers dtoUsers) throws JsonProcessingException {
        log.info("[test]updateUserDetail : {}, {}", userId, dtoUsers);
        Map<String, Object> zoomMap = new HashMap<>();
        DtoUsers db = dataService.readZoomUserAndUserByEmail(userId);
        if(db == null) { return ResponseEntity.notFound().build();}

        //test- 받아온 dto 자체에서 수정함.
        DtoUsers mergeUpdatedUser = dtoUsers.getUpdateUser(db);
        log.debug("[test]updateUserDetail mergeUpdatedUser: {}", mergeUpdatedUser);
        Map<String, Object> mergeUpdatedMap = dtoUsers.getUpdatedZoomUserMapByFive(db.getZoomUser(), new ArrayList<ZoomLicense>());
        log.debug("[test]updateUserDetail mergeUpdatedMap: {}", mergeUpdatedMap);

        ZoomUser zoomUser = dtoUsers.getZoomUser();
        User user = dtoUsers.getUser();
        log.debug("[test]updateUserDetail dbZoomUser: {}, zoomUser: {}, dbUser:{}, user: {}", db.getZoomUser(), zoomUser, db.getUser(), user);

        //줌 api 호출 : false -> true 면 api 비활성화 처리 // true -> false 면 활성화처리 // db 업데이트는 webhook 수신 후 자동처리됨.
        if(db.getZoomUser().isDeleted() != zoomUser.isDeleted() && zoomUser.isDeleted()) {
            ResponseEntity<String> response = this.deleteUser(userId);
            log.warn("[test]updateUserDetail deleteUser response: {}", response);
            db.getZoomUser().setDeleted(zoomUser.isDeleted());
        }

        String licenseName = zoomUser.getLicenseInfoList().getName();
        if(licenseName == null || ! licenseName.equals(db.getZoomUser().getLicenseInfoList().getName())) {
            ZoomLicense license = dataService.getZoomUserById(licenseName);
            zoomMap.put("type", license.getType());
            db.getZoomUser().setLicenseInfoList(license);
        }

        //줌 라이센스 api 호출 & 사용자정보 수정
        String displayName = zoomUser.getDisplayName();
        if(displayName == null || ! licenseName.equals(db.getZoomUser().getLicenseInfoList().getName())) {
            zoomMap.put("display_name", displayName);

        }
        db.getUser().setUsername(displayName);

        String position = user.getPosition();
        if(position == null || ! position.equals(db.getZoomUser().getJobTitle())) {
            zoomMap.put("job_title", position);
        }
        db.getUser().setPosition(position);

        Authority userType = user.getUserType();
        if(position == null || ! userType.getAuthorityName().equals(db.getUser().getUserType().getAuthorityName())) {
            zoomMap.put("role_name", userType.getAuthorityName());
        }
        db.getUser().setUserType(new Authority(userType.getAuthorityName()));

        String dept = zoomUser.getDept();
        if(dept == null || ! dept.equals(db.getZoomUser().getDept())) {
            zoomMap.put("dept", dept);
        }
        db.getUser().setDeptCode((dept.equals("개발연구소"))? "3" : db.getUser().getDeptCode());
        log.debug("[test] 개발연구소 아니면 수정안함 user.deptCode: {}", db.getUser().getDeptCode());

        log.debug("[test]updateUserDetail zoomMap: {}, companyUser: {}", zoomMap, db.getUser());
        //ResponseEntity<String> response = zoomApiService.api("/users/" + userId, HttpMethod.PATCH, zoomMap); //혹시 불필요 데이터 담아도 되나? : 라이센스는 name -> type 으로 변환 후 맵에 추가해야함.
        //log.debug("[test]updateUserDetail zoomApiService response: {}", response);

        ZoomUser updatedZoomUser = db.getZoomUser(); //api 가 진실이므로 캐시인 DB는 참조하지 않음. 사용자의 수정사항 체크용으로 임시 반환함.
        User updatedCompanyUser = dataService.saveUser(db.getUser());
        DtoUsers updated = new DtoUsers(updatedCompanyUser, updatedZoomUser);
        log.debug("[test]updateUserDetail updated: {}", updated);
        return ResponseEntity.ok(updated);
    }

}
