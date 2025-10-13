package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.common.SHA256Cipher;
import me.test.oauth.entity.UserList;
import me.test.oauth.service.UserListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** zoom 이 보낸 webhook 이벤트를 받기위한 컨트롤러 **/
@RestController
@RequestMapping("/webhook")
@PropertySource("classpath:zoom.properties")
@Slf4j
public class WebhookController {
    /** zoom webhook secret token : 검증을 위한 해싱 솔트키 **/
    @Value("${zoom.webhook.secret.token}")
    private String secretToken;

    /** Node.js 기준: 키 순서 유지, 들여쓰기 제거, null 포함하여 JSON 직력화 하기위함.**/
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    @Autowired
    private WebSocketController webSocketController;

    @Autowired
    private UserListService userListService;

    /** zoom 이 보낸 webhook 이벤트를 받고 즉시 200 OK 응답을 보냄.**/
    @PostMapping("/zoom")
    public ResponseEntity<String> zoomReceive(@RequestHeader("x-zm-signature") String signature, @RequestHeader("x-zm-request-timestamp") String timestamp, @RequestBody LinkedHashMap<String, Object> json) throws Exception {
        boolean isZoomWebhook = verifyWithZoomHeader(signature, timestamp, json);

        //이벤트 별 응답 전송.
        if(isZoomWebhook) {
            String event = (String)json.get("event");
            log.info("[test]zoomReceive {}", json);

            switch (event) {
                case "endpoint.url_validation":
                    String plainToken = (String)((LinkedHashMap<String, Object>)json.get("payload")).get("plainToken");
                    return ResponseEntity.ok(urlValidationResponseJSON(plainToken));
                case "meeting.end":
                case "meeting.participant.join":
                case "user.presence_status_updated":
                    log.info("[test]user.presence_status_updated");
                    Map<String,Object> object = (Map<String,Object>)((LinkedHashMap<String, Object>)json.get("payload")).get("object");
                    printJson(object);
                    updateStatus(object);
                default:
                    log.info("[test]event : {}", json);
                    return ResponseEntity.ok().build();
            }
        }

        return ResponseEntity.badRequest().build();
    }

    /** zoom 이 보낸게 맞는지 서명 검증 **/
    public boolean verifyWithZoomHeader(String signature, String timestamp, LinkedHashMap<String, Object> json) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(json);
        String hashingSignature = SHA256Cipher.generateZoomHmac(secretToken, timestamp, jsonBody);
        boolean isZoomWebhook = hashingSignature.equals(signature);
        log.info("[test]VerifyWithZoomHeader : {} isZoomWebhook : {}", hashingSignature, isZoomWebhook);
        return isZoomWebhook;
    }

    /** zoom 에 보낼 JSON 응답 생성 : 3s 이내 200 응답 필수**/
    public String urlValidationResponseJSON(String plainToken) throws Exception {
        String encryptedToken = SHA256Cipher.generateHmac(secretToken, plainToken);
        String responseJson = objectMapper.writeValueAsString(
                Map.of("plainToken", plainToken, "encryptedToken", encryptedToken)
        );
        log.info("[test]endpoint.url_validation responseJSon :{}", responseJson);
        return responseJson;
    }

    /** zoom 에서 받은 이벤트 출력**/
    public void printJson(Map<String,Object> object) throws JsonProcessingException {
        String data = objectMapper.writeValueAsString(object);
        log.info("[test]printJson : {}", data);
        webSocketController.enqueueEvent(object);
    }

    /** db update **/
    public boolean updateStatus(Map<String, Object>  event){
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
            webSocketController.enqueueException(error);
        }catch (Exception e){
            log.info("[test]updateStatus fail : {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("event", "error");
            error.put("message", String.format("maybe event data is not exist, check the event data : {}", event));
            error.put("detail", e.getMessage());
            webSocketController.enqueueException(error);
        }
        return false;
    }

}
