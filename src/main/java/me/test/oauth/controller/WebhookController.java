package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.common.SHA256Cipher;
import me.test.oauth.entity.UserList;
import me.test.oauth.entity.webhook.WebhookEvent;
import me.test.oauth.service.UserListService;
import me.test.oauth.service.WebhookEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    private WebhookEventService webhookEventService;

    @Autowired
    private APIController apiController;

    /** zoom 이 보낸 webhook 이벤트를 받고 즉시 200 OK 응답을 보냄.**/
    @PostMapping("/zoom")
    public ResponseEntity<String> zoomReceive(@RequestHeader("x-zm-signature") String signature, @RequestHeader("x-zm-request-timestamp") String timestamp, @RequestBody LinkedHashMap<String, Object> json) throws Exception {
        boolean isZoomWebhook = verifyWithZoomHeader(signature, timestamp, json);

        //이벤트 별 응답 전송.
        if(isZoomWebhook) {

            String event = (String)json.get("event");
            LinkedHashMap<String, Object> payload = (LinkedHashMap<String, Object>)json.get("payload");
            log.info("[test]zoomReceive {} : {}", event, json);

            //webhook 검증
            switch (event) {
                case "endpoint.url_validation":
                    String plainToken = (String) payload.get("plainToken");
                    return ResponseEntity.ok(urlValidationResponseJSON(plainToken));

                case "test.fail":
                    return ResponseEntity.badRequest().build();

                default: break;
            }

            //webhook -> DB
            WebhookEvent saved = saveWebhook(event, payload, json);

            //webhook -> API 호출 or DB 업데이트
            switch (event) {
                case "user.created":
                    String email = saved.getObject().getEmail();
                    boolean reloaded = reloadUser(email);
                    break;
                case "user.presence_status_updated":
                    updateStatus(payload);
                    break;
                case "meeting.participant.join":
                    break;
                default:
                    log.info("[test] default event : {}", json);
                    break;
            }

            //webhook -> 사용자
            sendJsonToWebSocket(payload, event);

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    //////////////////////////// webhook 검증

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

    //////////////////////////// webhook -> 사용자

    /** zoom 에서 받은 이벤트 출력**/
    public void sendJsonToWebSocket(LinkedHashMap<String, Object> payload, String queueType) throws JsonProcessingException {
        Map<String,Object> object = (Map<String,Object>)payload.get("object");
        object.put("queueType", queueType);
        webSocketController.enqueueEvent(object);

        log.debug("[debug]sendJsonToWebSocket : {}", objectMapper.writeValueAsString(object));
    }

    //////////////////////////// webhook -> DB

    /** 검증된 webhook 이벤트를 받으면, DB에 담아 로그를 남김**/
    public WebhookEvent saveWebhook(String event, LinkedHashMap<String, Object> payloadMap, LinkedHashMap<String, Object> json) throws JsonProcessingException {
        log.info("[test]saveWebhook : {}\n{}", event, payloadMap);
//        Payload payload = objectMapper.convertValue(payloadMap, Payload.class);
//        WebhookEvent webhookEvent = WebhookEvent.builder()
//                .event(event)
//                .payload(payload)
//                .build();
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

    /** 사용자 변경이 발생한 경우, 특정 사용자 목록을 다시 불러옴 **/
    public boolean reloadUser(String email) throws JsonProcessingException {
        UserList user = userListService.findByEmail(email);

        //DB 내 데이터 없으면 API 호출하고 해당 데이터 반환.
        if(user == null){
            user = apiController.getUser(email);
        }

        return userListService.save(user) != null;
    }

}
