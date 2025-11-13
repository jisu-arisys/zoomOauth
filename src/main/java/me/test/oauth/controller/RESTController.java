package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.dto.DtoOrganization;
import me.test.oauth.dto.DtoUsers;
import me.test.oauth.entity.*;
import me.test.oauth.service.DataService;
import me.test.oauth.service.ZoomApiService;
import org.springframework.data.repository.query.Param;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import me.test.oauth.common.RequestLatencyTracker;

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
    private final RequestLatencyTracker tracker;

    /** 한 사용자 기준 1분에 한번만 상태변경 요청. **/
    @PostMapping("/zoomUser/update/presence_status/{userId}")
    public ResponseEntity<String> setUserPresenceStatus(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) throws JsonProcessingException {
        log.info("[test]setUserPresenceStatus : {}", bodyMap);
        //api 로 존재하는 ID 체크 및 DB 정보저장
        ZoomUser user = dataService.zoomOps.getZoomUser(userId);
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
            list = dataService.zoomOps.patchByGetStatus();
            log.debug("[test] isForce zoomOps.patchByGetStatus() {}건 DB 중", list.size());
            list = dataService.readAllZoomUserNotDeleted();
            log.debug("[test] {}건 반환", list.size());
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            log.debug("[test] readOrGetAndSaveZoomUser()");
            list = dataService.zoomOps.readOrGetAndSaveZoomUser();
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
            user = dataService.zoomOps.getZoomUser(userId);
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            user = dataService.zoomOps.readOrGetAndSaveZoomUserByEmail(userId);
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
            users = dataService.zoomOps.patchByGetStatus();
        } else {
            //DB 조회 > 없으면 api 호출 후 DB 저장
            users = dataService.zoomOps.readOrGetAndSaveZoomUser();
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
    public ResponseEntity<String> updateZoomUser(@PathVariable String userId, @RequestBody Map<String, Object> bodyMap) {
        log.debug("[test]updateUserDetail : {}", userId);
        ResponseEntity<String> response = zoomApiService.api("/users/" + userId, HttpMethod.PATCH, bodyMap);
        return response;
    }

    ////// zoomUser & user 통합 데이터 관리

    @GetMapping("/userDetail")
    public ResponseEntity<List<DtoUsers>> getZoomUserVue() {
        List<DtoUsers> list = dataService.getZoomUserAndUser();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/userDetail/delete")
    public ResponseEntity<String> deleteZoomUserVue(@RequestBody String email) {
        tracker.start(email);
        log.info("[test]deleteZoomUserVue : {}", email);
        ResponseEntity<String> response = zoomApiService.api("/users/"+email, HttpMethod.DELETE, null);
        return response;
    }

    //동일사항이면 webhook 발생안함. 그냥 사용자 요청내용 다 담아서 보내면?
    @PostMapping("/userDetail/update/detail/{userId}")
    public ResponseEntity<DtoUsers> updateUserDetail(@PathVariable String userId, @RequestBody DtoUsers dtoUsers) {
        tracker.start(userId);
        log.info("[test]updateUserDetail : {}, {}", userId, dtoUsers.toString());
        //기존사용자 정보 불러오기
        DtoUsers db = dataService.getZoomUserAndUserByEmail(userId);
        if(db == null) { return ResponseEntity.notFound().build();}

        //zoomUser 변경사항 map 먼저 받기
        Map<String, Object> mergeUpdatedMap = dtoUsers.getUpdatedZoomUserMapByFive(db.getZoomUser());
        log.debug("[test]updateUserDetail mergeUpdatedMap: {}", mergeUpdatedMap.toString());
        //변경객체 받기
        DtoUsers mergeUpdatedDtoUser = dtoUsers.getUpdateUser(db);
        log.debug("[test]updateUserDetail mergeUpdatedDtoUser: {}", mergeUpdatedDtoUser.toString());

        //zoom api 호출
        ResponseEntity<String> response = zoomApiService.api("/users/" + userId, HttpMethod.PATCH, mergeUpdatedMap); //혹시 불필요 데이터 담아도 되나? : 라이센스는 name -> type 으로 변환 후 맵에 추가해야함.
        log.debug("[test]updateUserDetail zoomApiService response: {}", response);

        //사내정보 DB업데이트
        User updatedCompanyUser = dataService.saveUser(db.getUser());
        log.debug("[test]updateUserDetail updatedCompanyUser: {}", updatedCompanyUser.toString());

        log.debug("[test]updateUserDetail updated");
        return ResponseEntity.ok(mergeUpdatedDtoUser);
    }

    @GetMapping("/base/selectOption")
    public ResponseEntity<Map<String, List<?>>> getSelectOption() {
        // 한 트랜잭션으로 3개 쿼리 실행
        return ResponseEntity.ok(dataService.loadBaseOptions());
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<DtoOrganization>> getOrganizationList(){
        List<DtoOrganization> organizations = dataService.orgOps.getOrgJoinUser();
        log.debug("[test]getOrganizationList : {}", organizations.toString());
        return ResponseEntity.ok(organizations);
    }

    /** 사용자의 조직정보 변경에 대한 처리
     1. 화면단에서 받은 {depts:[], users:[]}
     2. List<User> 와 매칭되는 List<dtoUser> 조회
     3. 변경필드 추적해서 List<dtoUser> merge 반환, 변경항목 map 에 담기
     4. List<dtoUser> DB 저장 및 api 호출(map)
     5. List<Department> DB 저장.
     **/

    @PostMapping("/organizations/update")
    public ResponseEntity<List<DtoOrganization>> patchOrganizationList(@RequestBody  Map<String, Object> map){
        //데이터 추출 : {depts:[], users:[]}
        log.debug("[test]patchOrganizationList : {}", map.toString());
        List<Department> depts = objectMapper.convertValue(
                map.get("depts"), new TypeReference<List<Department>>() {}
        );
        List<User> users = objectMapper.convertValue(
                map.get("users"), new TypeReference<List<User>>() {}
        );
        //사용자 부서정보 업데이트 병합, DB 호출, API 호출
        List<DtoUsers> mergedDtoUsers = dataService.orgOps.patchByUser(users, null);

        //DB 업데이트
        List<Department> updateDepts = dataService.saveAllDept(depts);

        //변경내역 새로 조회해서 전달하기.
        return getOrganizationList();
    }

}
