package me.test.oauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.service.ZoomApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/** html 화면 및 model 출력을 위한 컨트롤러 **/
@Controller
@Slf4j
public class ViewController {

    @Autowired
    ZoomApiService zoomApiService;

    /** 기본화면 **/
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /** 사용자 기본화면 **/
    @GetMapping("/list")
    public String list() {
        return "users";
    }

    /** zoom 기본화면 **/
    @GetMapping("/zoom")
    public String zoom(Model model) {
        zoomApiService.setModelObject(model);
        return "zoom";
    }

    /** 모든 정보 초기화 **/
    @GetMapping("/zoom/resetToken")
    public String resetToken(Model model) {
        log.info("/resetToken");
        zoomApiService.resetToken(model);
        return "zoom";
    }

    /** 사용자권한 토큰 요청 **/
    @GetMapping("/zoom/client_credentials/token")
    public void clientCredentials(Model model, HttpServletResponse redirectResponse) {
        zoomApiService.clientCredentials(model, redirectResponse);
    }

    /** 사용자권한 토큰 추출 **/
    @RequestMapping(value="/zoom/get/auth" , method = {RequestMethod.GET, RequestMethod.POST})
    public String getZoomApiAuth(HttpServletRequest req, @RequestParam String code, Model model) {
        zoomApiService.getZoomApiAuth(req, code, model);
        zoomApiService.setModelObject(model);
        return "zoom";
    }

    /** 서버권한 토근 요청 및 추출 **/
    @GetMapping("/zoom/account_credentials/token")
    public String accountCredentials(Model model) {
        zoomApiService.accountCredentials(model);
        zoomApiService.setModelObject(model);
        return "zoom";
    }

    /** 지정한 사용자의 정보 혹은 모든 사용자의 정보를 조회합니다. **/
    @GetMapping("/zoom/users")
    public String getUsers(@RequestParam String userId, Model model) {
        String usersUrl = "/users/" + userId;
        log.info(usersUrl);
        String json = zoomApiService.getApi(usersUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("users", json);
        return "zoom";
    }

    /**사용자의 모든 스케줄러를 나열합니다. **/
    @GetMapping("/zoom/schedulers")
    public String getScheduledMeetingIdZoomApi(@RequestParam String userId, Model model) {
        String schedulesUrl = "/users/" + userId + "/schedulers";
        log.info(schedulesUrl);
        String json = zoomApiService.getApi(schedulesUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("schedulers", json);
        return "zoom";
    }

    /**사용자의 모든 회의정보를 검색합니다. **/
    @GetMapping("/zoom/meetings")
    public String getMeetingIdZoomApi(@RequestParam String userId, Model model) {
        String meetingUrl = "/users/" + userId + "/meetings";
        log.info(meetingUrl);
        String json = zoomApiService.getApi(meetingUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("meetings", json);
        return "zoom";
    }

    /** 전체 콜 목록을 조회합니다. **/
    @GetMapping("/zoom/phone/call_history")
    public String getCallHistoriesZoomApi(Model model) {
        String callsUrl = "/phone/call_history";
        log.info(callsUrl);
        String json = zoomApiService.getApi(callsUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("logs", json);
        return "zoom";
    }

    /** 콜 하나에 대한 자세한 정보를 조회합니다. **/
    @GetMapping("/zoom/phone/call_history_detail")
    public String getCallDetailZoomApi(@RequestParam String callLogId, Model model) {
        String callUrl = "/phone/call_history_detail/" + callLogId;
        log.info(callUrl);
        String json = zoomApiService.getApi(callUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("log", json);
        return "zoom";
    }

    /** 콜 하나에 대한 자세한 정보를 조회합니다. **/
    @GetMapping("/zoom/custom_action")
    public String getCustomApi(@RequestParam String action, Model model) {
        String callUrl = action;
        log.info(callUrl);
        String json = zoomApiService.getApi(callUrl);
        zoomApiService.setModelObject(model);
        model.addAttribute("log", json);
        return "zoom";
    }
}
