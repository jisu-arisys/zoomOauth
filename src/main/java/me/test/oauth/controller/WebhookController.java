package me.test.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.common.SHA256Cipher;
import me.test.oauth.entity.UserList;
import me.test.oauth.entity.webhook.WebhookEvent;
import me.test.oauth.service.DataService;
import me.test.oauth.service.WebSocketService;
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
    private WebSocketService webSocketService;

    @Autowired
    private DataService dataService;

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
            WebhookEvent saved = dataService.saveWebhook(event, payload, json);

            //webhook -> API 호출 or DB 업데이트
            switch (event) {
                case "user.created":
                    String email = saved.getObject().getEmail();
                    UserList reloaded = dataService.readUserOrGetUserAndSave(email);
                    break;
                case "user.presence_status_updated":
                    dataService.updateStatus(payload);
                    break;
                case "meeting.participant.join":
                    break;
                default:
                    log.info("[test] default event : {}", json);
                    break;
            }

            //webhook -> 사용자
            payload.put("queueType", event);
            webSocketService.enqueueEvent(payload);

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    //////////////////////////// webhook 검증

    /** zoom 이 보낸게 맞는지 서명 검증 **/
    private boolean verifyWithZoomHeader(String signature, String timestamp, LinkedHashMap<String, Object> json) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(json);
        String hashingSignature = SHA256Cipher.generateZoomHmac(secretToken, timestamp, jsonBody);
        boolean isZoomWebhook = hashingSignature.equals(signature);
        log.info("[test]VerifyWithZoomHeader : {} isZoomWebhook : {}", hashingSignature, isZoomWebhook);
        return isZoomWebhook;
    }

    /** zoom 에 보낼 JSON 응답 생성 : 3s 이내 200 응답 필수**/
    private String urlValidationResponseJSON(String plainToken) throws Exception {
        String encryptedToken = SHA256Cipher.generateHmac(secretToken, plainToken);
        String responseJson = objectMapper.writeValueAsString(
                Map.of("plainToken", plainToken, "encryptedToken", encryptedToken)
        );
        log.info("[test]endpoint.url_validation responseJSon :{}", responseJson);
        return responseJson;
    }
}
