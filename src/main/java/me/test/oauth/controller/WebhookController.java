package me.test.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.test.oauth.common.JsonUtil;
import me.test.oauth.common.SHA256Cipher;
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
public class WebhookController {


    /** zoom webhook secret token : 검증을 위한 해싱 솔트키 **/
    @Value("${zoom.webhook.secret.token}")
    private final String secretToken = "NbSl-uPgSEqW_eHMFUdewA";

    /** Node.js 기준: 키 순서 유지, 들여쓰기 제거, null 포함하여 JSON 직력화 하기위함.**/
    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    /** zoom 이 보낸 webhook 이벤트를 받고 즉시 200 OK 응답을 보냄.**/
    @PostMapping("/zoom")
    public ResponseEntity<String> zoomReceive(@RequestHeader("x-zm-signature") String signature, @RequestHeader("x-zm-request-timestamp") String timestamp, @RequestBody LinkedHashMap<String, Object> json) throws Exception {
        //받은 데이터 확인
        System.out.println("[test]zoomReceive "+json.toString());
        String event = (String)json.get("event");

        //zoom 이 보낸게 맞는지 서명 검증
        String jsonBody = objectMapper.writeValueAsString(json);
        System.out.println("[test]Node 형식으로 JSON 직력화 "+jsonBody);
        String hashingSignature = SHA256Cipher.generateZoomHmac(secretToken, timestamp, jsonBody);
        boolean isZoomWebhook = hashingSignature.equals(signature);
        System.out.println("[test]hashingSignature : " + hashingSignature + " isZoomWebhook : " + isZoomWebhook);

        //이벤트 별 응답 전송.
        if(isZoomWebhook) {
            switch (event) {
                case "endpoint.url_validation":
                    // 3s 이내 200 응답 필수
                    Map<String, Object> payload = (LinkedHashMap<String, Object>)json.get("payload");
                    String plainToken = (String)payload.get("plainToken");
                    String hashForValidate = SHA256Cipher.generateHmac(secretToken, plainToken);
                    String responseJson = objectMapper.writeValueAsString(
                            Map.of("plainToken", plainToken, "encryptedToken", hashForValidate)
                    );
                    System.out.println("[test]endpoint.url_validation responseJSon :"+responseJson);
                    return ResponseEntity.ok(responseJson.toString());
                case "meeting.end":
                case "meeting.participant.join":
                case "meeting.participant.leave":
                default:
                    System.out.println(json.toString());
                    return ResponseEntity.ok().build();
            }
        }

        return ResponseEntity.badRequest().build();
    }

}
