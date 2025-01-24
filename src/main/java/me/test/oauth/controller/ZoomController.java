package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**controller 어노테이션에 의해 스프링 빈으로 등록 되어야, 컴포넌트 스캔의 대상이 되어 value 어노테이션이 동작한다.**/
@Controller()
@RequestMapping("/zoom")
@PropertySource("classpath:zoom.properties")
public class ZoomController {

    /** HTTP 통신을 위한 도구로 REST full API 웹 서비스와의 상호작용을 쉽게 외부 도메인에서 데이터를 가져오거나 전송할 때 사용되는 스프링 프레임워크의 클래스**/
    private final RestTemplate restTemplate = new RestTemplate();
    /** 권한요청용 base url **/
    @Value("${zoom.oauth.endpoint}")
    private String zoomOauthEndpoint;
    /** api 호출용 base url **/
    @Value("${zoom.api.base.url}")
    private String zoomApiBaseUrl;


//    사용자 권한받기
    @Value("${ZOOM_CLIENT_ID}")
    private String zoomClientId;
    @Value("${ZOOM_CLIENT_SECRET}")
    private String zoomClientSecret;
    @Value("${ZOOM_REDIRECT_URI}")
    private String ZOOM_REDIRECT_URI;
    private String CLIENT_CREDENTIALS_ACCESS_TOKEN;
    private String AUTHORIZATION_CODE;
    private String USER_ACCESS_TOKEN;
    private String USER_REFRESH_TOKEN;

//    서버 권한받기
    @Value("${ZOOM_SERVER_ACCOUNT_ID}")
    private String zoomAccountId;
    @Value("${ZOOM_SERVER_CLIENT_ID}")
    private String zoomServerClientId;
    @Value("${ZOOM_SERVER_CLIENT_SECRET}")
    private String zoomServerClientSecret;
    private String ACCOUNT_CREDENTIALS_ACCESS_TOKEN;

//    사용자 권한과 서버 권한의 공동사용.
    private HttpHeaders tokenHeaders = new HttpHeaders();

    /** 사용자 권한을 받기위한 헤더 **/
    private HttpHeaders getClientHeaders() {
        //서버 권한 정보 초기화 : 동시사용 불가.
        ACCOUNT_CREDENTIALS_ACCESS_TOKEN = null;
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.setBasicAuth(zoomClientId, zoomClientSecret, StandardCharsets.UTF_8);
        return tokenHeaders;
    }

    /** 서버 권한을 받기위한 헤더 **/
    private HttpHeaders getServerHeaders() {
        //사용자 권한 정보 초기화 : 동시사용 불가.
        CLIENT_CREDENTIALS_ACCESS_TOKEN = null;
        AUTHORIZATION_CODE = null;
        USER_ACCESS_TOKEN = null;
        USER_REFRESH_TOKEN = null;

        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.setBasicAuth(zoomServerClientId, zoomServerClientSecret, StandardCharsets.UTF_8);
        return tokenHeaders;
    }

    /** 부여받은 권한을 담은 헤더 : 사용자 권한과 서버 권한의 공동사용.**/
    private void setTokenHeaders(String token) {
        tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.setBearerAuth(token);
    }

    private void setModelObject(Model model) {
        model.addAttribute("clientToken", CLIENT_CREDENTIALS_ACCESS_TOKEN !=null? CLIENT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("accountToken", ACCOUNT_CREDENTIALS_ACCESS_TOKEN !=null? ACCOUNT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("authorizationCode",  AUTHORIZATION_CODE !=null? AUTHORIZATION_CODE : "");
        model.addAttribute("accessToken", USER_ACCESS_TOKEN !=null? USER_ACCESS_TOKEN : "");
        model.addAttribute("refreshToken", USER_REFRESH_TOKEN !=null? USER_REFRESH_TOKEN : "");
        model.addAttribute("isSuccess", CLIENT_CREDENTIALS_ACCESS_TOKEN!=null || ACCOUNT_CREDENTIALS_ACCESS_TOKEN!=null || USER_ACCESS_TOKEN!=null);
    }

    /** 기본화면 **/
    @GetMapping()
    public String index(Model model) {
        System.out.println("basic /zoom");
        setModelObject(model);
        return "zoom";
    }

    /** 모든 정보 초기화 **/
    @GetMapping("/resetToken")
    public String resetToken(Model model) {
        System.out.println("/resetToken");
        CLIENT_CREDENTIALS_ACCESS_TOKEN = null;
        ACCOUNT_CREDENTIALS_ACCESS_TOKEN = null;
        AUTHORIZATION_CODE = null;
        USER_ACCESS_TOKEN = null;
        USER_REFRESH_TOKEN = null;
        setModelObject(model);
        return "zoom";
    }

//  사용자권한 토큰 요청

    /** 애플리케이션 자체가 API 에 액세스해야 할 때
     * clientCredentials > authorize : 리다이렉트 > get/auth > token
     * 성공적인 응답
     * {"access_token":"~","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"<a href="https://api.zoom.us">...</a>"}
     *
     * 성공시, 쿼리 매개변수를 사용하여 인증 코드를 요청합니다.
     * <br>redirect_uri 권한이 부여된 경우 Zoom 은 코드 쿼리 매개변수에 권한 부여 코드를 사용하여 사용자를 리디렉션합니다 .
     * <br>https://{{ZOOM_REDIRECT_URI}}?code=~
     **/
    @GetMapping("/client_credentials/token")
    public void clientCredentials(Model model, HttpServletResponse redirectResponse) {
        UriComponentsBuilder clientBuilder = UriComponentsBuilder.fromHttpUrl(zoomOauthEndpoint + "/token")
                .queryParam("grant_type", "client_credentials");
        String client_credentialsUrl = clientBuilder.build().toUriString();
        HttpHeaders clientHeaders = getClientHeaders();
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(clientHeaders);
        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    client_credentialsUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            System.out.println("client - response: " + response.getStatusCode() + response.getBody());

            // 토큰 저장
            String responseBody = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            CLIENT_CREDENTIALS_ACCESS_TOKEN = jsonNode.get("access_token").asText();


            UriComponentsBuilder authBuilder = UriComponentsBuilder.fromHttpUrl(zoomOauthEndpoint + "/authorize")
                    .queryParam("response_type", "code")
                    .queryParam("redirect_uri", ZOOM_REDIRECT_URI)
                    .queryParam("client_id", zoomClientId);
              /*    선택사항.
                .queryParam("state", "")
                .queryParam("code_challenge", "")
                .queryParam("code_challenge_method", "plain"); */

            String redirectUri = authBuilder.build().toUriString();

            try{
                redirectResponse.sendRedirect(redirectUri);
                System.out.println("authorize - sendRedirect");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (HttpClientErrorException e) {
            String messages = "client - error: " +
                    e.getStatusCode().value() +
                    e.getResponseBodyAsString() +
                    "clientHeaders \n" +
                    clientHeaders +
                    "client_credentialsUrl \n" +
                    client_credentialsUrl;

            System.out.println(messages);
            model.addAttribute("messages",messages);
        } catch (JsonProcessingException e) {
            model.addAttribute("messages",e.toString());
        }
    }

    /**
     *  client_credentials 연계
     * /authorize 요청에 대한 승인이 나면, 코드정보가 담긴 해당 페이지(어플리케이션에 지정된 리다이렉션 경로)로 리다이렉션된다.
     * 쿼리파라매터에서 코드정보를 추출해 저장한다.
     *
     * 성공시, 받은 AUTHORIZATION_CODE 를 가지고 액세스토근을 요청한다.컨텐츠 타입 주의!
     *      * -d "grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=ZOOM_REDIRECT_URI"
     *<br>성공적인 응답
     *<br>{
     *<br>"access_token": "~",
     *<br>"token_type": "bearer",
     *<br>"refresh_token": "~",
     *<br>"expires_in": 3599,
     *<br>"scope": "user:read"
     *<br>}
     * **/
    @RequestMapping(value="/get/auth" , method = {RequestMethod.GET, RequestMethod.POST})
    public String getZoomApiAuth(HttpServletRequest req, @RequestParam String code, Model model) {
        System.out.println("redirect_uri: "+ "zoom/get/auth");

        if(req.getParameter("code") != null) {
            AUTHORIZATION_CODE = req.getParameter("code");

            //성공적으로 code 를 받으면 token 요청을 한다
            String tokenUrl = zoomOauthEndpoint + "/token";

            // 바디생성
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", AUTHORIZATION_CODE);
            requestBody.add("redirect_uri", ZOOM_REDIRECT_URI);
            /* 선택사항
            requestBody.add("code_verifier", URLEncoder.encode(AUTHORIZATION_CODE)); //400 Bad Request, invalid_request
            requestBody.add("device_code", "");*/

            HttpHeaders clientHeaders = getClientHeaders();
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, clientHeaders);

            try{
                ResponseEntity<String> response = restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                System.out.println("token - response: " + response.getStatusCode() + response.getBody());

                // 토큰 저장
                String responseBody  = response.getBody();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);
                USER_ACCESS_TOKEN = jsonNode.get("access_token").asText();
                USER_REFRESH_TOKEN = jsonNode.get("refresh_token").asText();
                setTokenHeaders(USER_ACCESS_TOKEN);
                System.out.println("access_token : " + USER_ACCESS_TOKEN);

            } catch (HttpClientErrorException e) {
                String messages = "token - error: " +
                        e.getStatusCode().value() +
                        e.getResponseBodyAsString() +
                        "clientHeaders \n" +
                        clientHeaders +
                        "requestBody \n" +
                        requestBody;

                System.out.println(messages);
                model.addAttribute("messages",messages);
            } catch (JsonProcessingException e) {
                model.addAttribute("messages",e.toString());
            }
        }else {
            model.addAttribute("messages", "Authorize Code is null");
        }
        System.out.printf("code: %s", code);

        setModelObject(model);
        return "zoom";
    }

//  서버권한 토큰 요청

    /** 애플리케이션 소유자의 액세스 토큰입니다. 서버에서 API 에 액세스해야 할 때 (사용자 인증이 필요하지 않은 경우)
     * 새로 고침 토큰이 없습니다. 한 시간 후에 만료됩니다.
     * 계정 관리자는 이러한 앱 유형을 빌드하는 개발자가 사용할 수 있는 범위를 승인합니다.
     * 성공적인 응답
     * {"access_token":"~","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"<a href="https://api.zoom.us">...</a>"}
     * **/
    @GetMapping("/account_credentials/token")
    public String accountCredentials(Model model) {
        String account_credentialsUrl =zoomOauthEndpoint + "/token";

        // 바디생성
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "account_credentials");
        requestBody.add("account_id", zoomAccountId);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, getServerHeaders());

        try{
            ResponseEntity<String> response = restTemplate.exchange(
                    account_credentialsUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            System.out.println("account_credentials - response: " + response.getStatusCode() + response.getBody());

            // 토큰 저장
            String responseBody = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            ACCOUNT_CREDENTIALS_ACCESS_TOKEN = jsonNode.get("access_token").asText();
            setTokenHeaders(ACCOUNT_CREDENTIALS_ACCESS_TOKEN);

        } catch (HttpClientErrorException e) {
            String messages = "account - error: " +
                    e.getStatusCode().value() +
                    e.getResponseBodyAsString() +
                    "tokenHeaders \n" +
                    tokenHeaders +
                    "account_credentialsUrl \n" +
                    account_credentialsUrl;
            model.addAttribute("messages",messages);

        } catch (JsonProcessingException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

//    토큰을 가지고 api GET 요청.

    /** 전역변수 tokenRequestEntity 를 사용하여 GET 요청을 보냅니다.
     *
     * <br> 요청헤더에 엑세스 토큰이 필요합니다.
     * <br> -H "Authorization: Bearer USER_ACCESS_TOKEN"
     * <br> -H "Content-Type: application/x-www-form-urlencoded"
     * **/
    public void getApi(String getUrl, String attributeName, Model model) {
        // 요청 가능여부 검증
        if(tokenHeaders == null) {
            System.out.printf("tokenHeaders is null. fail to get api %s\n", getUrl);
            return;
        }

        try {
            HttpEntity<Map<String, Object>> tokenRequestEntity = new HttpEntity<>(tokenHeaders);
            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl,
                    HttpMethod.GET,
                    tokenRequestEntity,
                    String.class
            );

            // 응답 바디 JSON 형로 출력 : 4개의 공백으로 들여쓰기
            ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Object jsonObject = objectMapper.readValue(response.getBody(), Object.class);
            String prettyJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

            System.out.println(prettyJsonString);

            // 모델에 데이터 추가
            model.addAttribute(attributeName, prettyJsonString);

        } catch (HttpClientErrorException e) {
            // 예외 처리
            model.addAttribute("messages", e.toString());
        } catch (JsonProcessingException e) {
            model.addAttribute("messages", e.toString());
        }
        setModelObject(model);
    }


    /** 지정한 사용자의 정보 혹은 모든 사용자의 정보를 조회합니다.
     * <br><a href="https://developers.zoom.us/docs/api/users/#tag/users/GET/users/">...</a>{userId}
     * <br>성공시 응답 get a user: {"id":"~","first_name":"지수","last_name":"엄","display_name":"엄지수","email":"asha.jisu@gmail.com","type":1,"role_name":"Owner","pmi":8603532991,"use_pmi":false,"personal_meeting_url":"<a href="https://us04web.zoom.us/j/8603532991?pwd=uWaoSSmdvXaOHs6xYst7S3664Xa4Eu.1">...</a>","timezone":"Asia/Seoul","verified":0,"dept":"","created_at":"2020-07-12T04:17:16Z","last_login_time":"2024-12-18T05:47:59Z","last_client_version":"5.12.9.10650(win)","pic_url":"<a href="https://us04web.zoom.us/p/v2/...">...</a>","cms_user_id":"","jid":"g9zvjgl3rcq1anovgch2mg@xmpp.zoom.us","group_ids":[],"im_group_ids":[],"account_id":"~","language":"ko-KO","phone_country":"","phone_number":"","status":"active","job_title":"","cost_center":"","location":"","login_types":[1],"role_id":"0","cluster":"us04","user_created_at":"2020-07-12T04:17:16Z"}
     * <br>성공시 응답 List users : {"page_count":1,"page_number":1,"page_size":30,"total_records":1,"next_page_token":"","users":[{"id":"~","first_name":"지수","last_name":"엄","display_name":"엄지수","email":"asha.jisu@gmail.com","type":1,"pmi":8603532991,"timezone":"Asia/Seoul","verified":0,"dept":"","created_at":"2020-07-12T04:17:16Z","last_login_time":"2024-12-18T05:47:59Z","last_client_version":"5.12.9.10650(win)","pic_url":"<a href="https://us04web.zoom.us/p/v2/d0ba9e75f6f34051ceedc6ef868ddb29e10d8c34a0c16cfa232db3a0d04b91a1/d049bd1e-7fb5-46f7-ba29-5b058223a1dd-641">...</a>","language":"ko-KO","phone_number":"","status":"active","role_id":"0","user_created_at":"2020-07-12T04:17:16Z"}]}
     * **/
    @GetMapping("/users")
    public String getUsers(@RequestParam String userId, Model model) {
        String usersUrl = zoomApiBaseUrl + "/users/" + userId;
        System.out.println(usersUrl);
        getApi(usersUrl, "users", model);
        return "zoom";
    }

    /**사용자의 모든 스케줄러를 나열합니다. **/
    @GetMapping("/schedulers")
    public String getScheduledMeetingIdZoomApi(@RequestParam String userId, Model model) {
        String schedulesUrl = zoomApiBaseUrl + "/users/" + userId + "/schedulers";
        System.out.println(schedulesUrl);
        getApi(schedulesUrl, "schedulers", model);
        return "zoom";
    }

    /**사용자의 모든 회의정보를 검색합니다. **/
    @GetMapping("/meetings")
    public String getMeetingIdZoomApi(@RequestParam String userId, Model model) {
        String meetingUrl = zoomApiBaseUrl + "/users/" + userId + "/meetings";
        System.out.println(meetingUrl);
        getApi(meetingUrl, "meetings", model);
        return "zoom";
    }

    /** 전체 콜 목록을 조회합니다. **/
    @GetMapping("/phone/call_history")
    public String getCallHistoriesZoomApi(Model model) {
        String callsUrl = zoomApiBaseUrl + "/phone/call_history";
        System.out.println(callsUrl);
        getApi(callsUrl, "logs", model);
        return "zoom";
    }

    /** 콜 하나에 대한 자세한 정보를 조회합니다. **/
    @GetMapping("/phone/call_history_detail")
    public String getCallDetailZoomApi(@RequestParam String callLogId, Model model) {
        String callUrl = zoomApiBaseUrl + "/phone/call_history_detail/" + callLogId;
        System.out.println(callUrl);
        getApi(callUrl, "log", model);
        return "zoom";
    }
}
