package me.test.oauth.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

    /**HTTP 통신을 위한 도구로 RESTful API 웹 서비스와의 상호작용을 쉽게 외부 도메인에서 데이터를 가져오거나 전송할 때 사용되는 스프링 프레임워크의 클래스**/
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${zoom.api.base.url}")
    private String zoomApiBaseUrl;
    @Value("${zoom.oauth.endpoint}")
    private String zoomOauthEndpoint;

//    사용자 권한받기
    @Value("${ZOOM_CLIENT_ID}")
    private String zoomClientId;
    @Value("${ZOOM_CLIENT_SECRET}")
    private String zoomClientSecret;
    @Value("${ZOOM_REDIRECT_URI}")
    private String ZOOM_REDIRECT_URI;

//    서버 권한받기
    @Value("${ZOOM_SERVER_ACCOUNT_ID}")
    private String zoomAccountId;
    @Value("${ZOOM_SERVER_CLIENT_ID}")
    private String zoomServerClientId;
    @Value("${ZOOM_SERVER_CLIENT_SECRET}")
    private String zoomServerClientSecret;

    private String CLIENT_CREDENTIALS_ACCESS_TOKEN;
    private String ACCOUNT_CREDENTIALS_ACCESS_TOKEN;
    private String AUTHORIZATION_CODE;
    private String USER_ACCESS_TOKEN;
    private String USER_REFRESH_TOKEN;

    private HttpHeaders headers;
    private HttpHeaders tokenHeaders;
    private HttpEntity<Map<String, Object>> tokenRequestEntity;

    /** 기본화면 **/
    @GetMapping()
    public String index(Model model) {
        System.out.println("basic /zoom");
        setModelObject(model);
        return "zoom";
    }

    /** 의존성 주입이 이루어진 후 자동호출되어 초기화를 수행하는 메서드
     *<br> 사용자 인증을 받고 인증코드를 얻기위한 헤더를 생성합니다.
     <br>     -H "Authorization: Basic BASE64_ENCODED_CLIENT_ID_AND_SECRET" \
     <br>     -H "Content-Type: application/x-www-form-urlencoded" \
     * **/
    @PostConstruct
    public void init() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(zoomClientId, zoomClientSecret, StandardCharsets.UTF_8);
    }

    private void setTokenHeaders(String token) {
        tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        tokenHeaders.setBearerAuth(token);
        tokenRequestEntity = new HttpEntity<>(tokenHeaders);
    }

    private HttpHeaders getServerHeaders() {
        HttpHeaders serverHeaders = new HttpHeaders();
        serverHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        serverHeaders.setBasicAuth(zoomServerClientId, zoomServerClientSecret, StandardCharsets.UTF_8);
        return serverHeaders;
    }

    private void setModelObject(Model model) {
        model.addAttribute("clientToken", CLIENT_CREDENTIALS_ACCESS_TOKEN !=null? CLIENT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("accountToken", ACCOUNT_CREDENTIALS_ACCESS_TOKEN !=null? ACCOUNT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("authorizationCode",  AUTHORIZATION_CODE !=null? AUTHORIZATION_CODE : "");
        model.addAttribute("accessToken", USER_ACCESS_TOKEN !=null? USER_ACCESS_TOKEN : "");
        model.addAttribute("refreshToken", USER_REFRESH_TOKEN !=null? USER_REFRESH_TOKEN : "");
        model.addAttribute("isSuccess", CLIENT_CREDENTIALS_ACCESS_TOKEN!=null || ACCOUNT_CREDENTIALS_ACCESS_TOKEN!=null || USER_ACCESS_TOKEN!=null);
        model.addAttribute("isSuccess", CLIENT_CREDENTIALS_ACCESS_TOKEN!=null || ACCOUNT_CREDENTIALS_ACCESS_TOKEN!=null || USER_ACCESS_TOKEN!=null);
    }

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

    /**
     * 쿼리 매개변수를 사용하여 인증 코드를 요청합니다.
     * <br>redirect_uri권한이 부여된 경우 Zoom은 코드 쿼리 매개변수에 권한 부여 코드를 사용하여 사용자를 리디렉션합니다 .
     * <br>https://{{ZOOM_REDIRECT_URI}}?code=obBEe8ewaL_KdyNjniT4KPd8ffDWt9fGB
     **/
    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(zoomOauthEndpoint + "/authorize")
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", ZOOM_REDIRECT_URI)
                .queryParam("client_id", zoomClientId);
              /*    선택사항.
                .queryParam("state", "")
                .queryParam("code_challenge", "")
                .queryParam("code_challenge_method", "plain"); */

        String redirectUri = builder.build().toUriString();

        try{
            response.sendRedirect(redirectUri);
            System.out.println("authorize - sendRedirect");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 어플리케이션에 지정된 리다이렉션 경로이다.
     * /authorize 요청에 대한 승인이 나면, 코드정보가 담긴 해당 페이지로 리다이렉션된다.
     * 쿼리파라매터에서 코드정보를 추출해 저장한다. 그리고 토큰정보를 담아 index 페이지를 연다.
     * **/
    @RequestMapping(value="/get/auth" , method = {RequestMethod.GET, RequestMethod.POST})
    public String getZoomApiAuth(HttpServletRequest req, @RequestParam String code, Model model) {
        System.out.println("redirect_uri: "+ "zoom/get/auth");

        if(req.getParameter("code") != null) {
            AUTHORIZATION_CODE = req.getParameter("code");
        }else {
            model.addAttribute("messages", "Authorize Code is null");
        }
        System.out.println("code: "+ code);

        setModelObject(model);
        return "zoom";
    }

    /** client_credentials
     * 컨텐츠 타입 주의!
     * 받은 AUTHORIZATION_CODE 를 가지고 액세스토근을 요청함.
     *      * -d "grant_type=authorization_code&code=AUTHORIZATION_CODE&redirect_uri=ZOOM_REDIRECT_URI"
     *<br>성공적인 응답
     *<br>{
     *<br>"access_token": "eyJhbGciOiJIUzUxMiIsInYiOiIyLjAiLCJraWQiOiI8S0lEPiJ9.eyJ2ZXIiOiI2IiwiY2xpZW50SWQiOiI8Q2xpZW50X0lEPiIsImNvZGUiOiI8Q29kZT4iLCJpc3MiOiJ1cm46em9vbTpjb25uZWN0OmNsaWVudGlkOjxDbGllbnRfSUQ-IiwiYXV0aGVudGljYXRpb25JZCI6IjxBdXRoZW50aWNhdGlvbl9JRD4iLCJ1c2VySWQiOiI8VXNlcl9JRD4iLCJncm91cE51bWJlciI6MCwiYXVkIjoiaHR0cHM6Ly9vYXV0aC56b29tLnVzIiwiYWNjb3VudElkIjoiPEFjY291bnRfSUQ-IiwibmJmIjoxNTgwMTQ2OTkzLCJleHAiOjE1ODAxNTA1OTMsInRva2VuVHlwZSI6ImFjY2Vzc190b2tlbiIsImlhdCI6MTU4MDE0Njk5MywianRpIjoiPEpUST4iLCJ0b2xlcmFuY2VJZCI6MjV9.F9o_w7_lde4Jlmk_yspIlDc-6QGmVrCbe_6El-xrZehnMx7qyoZPUzyuNAKUKcHfbdZa6Q4QBSvpd6eIFXvjHw",
     *<br>"token_type": "bearer",
     *<br>"refresh_token": "eyJhbGciOiJIUzUxMiIsInYiOiIyLjAiLCJraWQiOiI8S0lEPiJ9.eyJ2ZXIiOiI2IiwiY2xpZW50SWQiOiI8Q2xpZW50X0lEPiIsImNvZGUiOiI8Q29kZT4iLCJpc3MiOiJ1cm46em9vbTpjb25uZWN0OmNsaWVudGlkOjxDbGllbnRfSUQ-IiwiYXV0aGVudGljYXRpb25JZCI6IjxBdXRoZW50aWNhdGlvbl9JRD4iLCJ1c2VySWQiOiI8VXNlcl9JRD4iLCJncm91cE51bWJlciI6MCwiYXVkIjoiaHR0cHM6Ly9vYXV0aC56b29tLnVzIiwiYWNjb3VudElkIjoiPEFjY291bnRfSUQ-IiwibmJmIjoxNTgwMTQ2OTkzLCJleHAiOjIwNTMxODY5OTMsInRva2VuVHlwZSI6InJlZnJlc2hfdG9rZW4iLCJpYXQiOjE1ODAxNDY5OTMsImp0aSI6IjxKVEk-IiwidG9sZXJhbmNlSWQiOjI1fQ.Xcn_1i_tE6n-wy6_-3JZArIEbiP4AS3paSD0hzb0OZwvYSf-iebQBr0Nucupe57HUDB5NfR9VuyvQ3b74qZAfA",
     *<br>"expires_in": 3599,
     *<br>"scope": "user:read"
     *<br>}
     * **/
    @GetMapping("/token")
    public String token(Model model) {
        String tokenUrl = zoomOauthEndpoint + "/token";
        // 바디생성
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", AUTHORIZATION_CODE);
        requestBody.add("redirect_uri", ZOOM_REDIRECT_URI);
        /* 선택사항
        requestBody.add("code_verifier", URLEncoder.encode(AUTHORIZATION_CODE)); //400 Bad Request, invalid_request
        requestBody.add("device_code", "");*/

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

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
                    "headers \n" +
                    headers +
                    "requestBody \n" +
                    requestBody;

            System.out.println(messages);
            model.addAttribute("messages",messages);
        } catch (JsonProcessingException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

    /** 애플리케이션 자체가 API에 액세스해야 할 때 (사용자 인증이 필요하지 않은 경우)
     * 성공적인 응답
     * {"access_token":"eyJzdiI6IjAwMDAwMiIsImFsZyI6IkhTNTEyIiwidiI6IjIuMCIsImtpZCI6IjM0MDNiYjVjLTlmNmQtNDI5My1iNjRmLTIwYmNlMzdmZjM1ZSJ9.eyJhdWQiOiJodHRwczovL29hdXRoLnpvb20udXMiLCJ1aWQiOiJHOVp2SkdMM1JDcTFhbk9WZ2NoMk1nIiwidmVyIjoxMCwiYXVpZCI6IjNkYTk1ZTNlYjY4ZTNmMTY5ZGQ5OTNkZWZjMjdiYTUxMmY2NzhkMjU1ZDUyYjdkYWIxODg5NmVjYWI2N2E0ZDIiLCJuYmYiOjE3MzQ1MDE4MDAsImlzcyI6InptOmNpZDpjUTBPVXN1YVFxVWQ4cnZncVAwREEiLCJnbm8iOjAsImV4cCI6MTczNDUwNTQwMCwidHlwZSI6MiwiaWF0IjoxNzM0NTAxODAwfQ.4zUSp5Iy_RMU70nhLCdMpJLA8U4_P_Nim_8C5QWDyJYx9QX-xosX-293Gu-Oa4vYkH5w7ajJZQ1APmQIrTBv_w","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"https://api.zoom.us"}
     * **/
    @GetMapping("/client_credentials/token")
    public String clientCredentials(Model model) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(zoomOauthEndpoint + "/token")
                .queryParam("grant_type", "client_credentials");
        String client_credentialsUrl = uriBuilder.build().toUriString();
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(getServerHeaders());
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
            setTokenHeaders(CLIENT_CREDENTIALS_ACCESS_TOKEN);

        } catch (HttpClientErrorException e) {
            String messages = "client - error: " +
                    e.getStatusCode().value() +
                    e.getResponseBodyAsString() +
                    "headers \n" +
                    headers +
                    "client_credentialsUrl \n" +
                    client_credentialsUrl;

            System.out.println(messages);
            model.addAttribute("messages",messages);
        } catch (JsonProcessingException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

    /** 애플리케이션 소유자의 액세스 토큰입니다. 서버에서 API 에 액세스해야 할 때 (사용자 인증이 필요하지 않은 경우)
     * 새로 고침 토큰이 없습니다. 한 시간 후에 만료됩니다.
     * 계정 관리자는 이러한 앱 유형을 빌드하는 개발자가 사용할 수 있는 범위를 승인합니다.
     * 성공적인 응답
     * {"access_token":"eyJzdiI6IjAwMDAwMiIsImFsZyI6IkhTNTEyIiwidiI6IjIuMCIsImtpZCI6IjM0MDNiYjVjLTlmNmQtNDI5My1iNjRmLTIwYmNlMzdmZjM1ZSJ9.eyJhdWQiOiJodHRwczovL29hdXRoLnpvb20udXMiLCJ1aWQiOiJHOVp2SkdMM1JDcTFhbk9WZ2NoMk1nIiwidmVyIjoxMCwiYXVpZCI6IjNkYTk1ZTNlYjY4ZTNmMTY5ZGQ5OTNkZWZjMjdiYTUxMmY2NzhkMjU1ZDUyYjdkYWIxODg5NmVjYWI2N2E0ZDIiLCJuYmYiOjE3MzQ1MDE4MDAsImlzcyI6InptOmNpZDpjUTBPVXN1YVFxVWQ4cnZncVAwREEiLCJnbm8iOjAsImV4cCI6MTczNDUwNTQwMCwidHlwZSI6MiwiaWF0IjoxNzM0NTAxODAwfQ.4zUSp5Iy_RMU70nhLCdMpJLA8U4_P_Nim_8C5QWDyJYx9QX-xosX-293Gu-Oa4vYkH5w7ajJZQ1APmQIrTBv_w","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"https://api.zoom.us"}
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
                    "headers \n" +
                    headers +
                    "account_credentialsUrl \n" +
                    account_credentialsUrl;
            model.addAttribute("messages",messages);

        } catch (JsonProcessingException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

    /**Get a user & List users
     * <br>https://developers.zoom.us/docs/api/users/#tag/users/GET/users/{userId}
     * <br> 요청헤더에 엑세스 토큰이 필요합니다.
     * <br> -H "Authorization: Bearer USER_ACCESS_TOKEN"
     * <br> -H "Content-Type: application/x-www-form-urlencoded"
     * <br>성공시 응답 get a user: {"id":"G9ZvJGL3RCq1anOVgch2Mg","first_name":"지수","last_name":"엄","display_name":"엄지수","email":"asha.jisu@gmail.com","type":1,"role_name":"Owner","pmi":8603532991,"use_pmi":false,"personal_meeting_url":"https://us04web.zoom.us/j/8603532991?pwd=uWaoSSmdvXaOHs6xYst7S3664Xa4Eu.1","timezone":"Asia/Seoul","verified":0,"dept":"","created_at":"2020-07-12T04:17:16Z","last_login_time":"2024-12-18T05:47:59Z","last_client_version":"5.12.9.10650(win)","pic_url":"https://us04web.zoom.us/p/v2/d0ba9e75f6f34051ceedc6ef868ddb29e10d8c34a0c16cfa232db3a0d04b91a1/d049bd1e-7fb5-46f7-ba29-5b058223a1dd-641","cms_user_id":"","jid":"g9zvjgl3rcq1anovgch2mg@xmpp.zoom.us","group_ids":[],"im_group_ids":[],"account_id":"wyIaxFbtSt24INWz5OOJCA","language":"ko-KO","phone_country":"","phone_number":"","status":"active","job_title":"","cost_center":"","location":"","login_types":[1],"role_id":"0","cluster":"us04","user_created_at":"2020-07-12T04:17:16Z"}
     * <br>성공시 응답 List users : {"page_count":1,"page_number":1,"page_size":30,"total_records":1,"next_page_token":"","users":[{"id":"G9ZvJGL3RCq1anOVgch2Mg","first_name":"지수","last_name":"엄","display_name":"엄지수","email":"asha.jisu@gmail.com","type":1,"pmi":8603532991,"timezone":"Asia/Seoul","verified":0,"dept":"","created_at":"2020-07-12T04:17:16Z","last_login_time":"2024-12-18T05:47:59Z","last_client_version":"5.12.9.10650(win)","pic_url":"https://us04web.zoom.us/p/v2/d0ba9e75f6f34051ceedc6ef868ddb29e10d8c34a0c16cfa232db3a0d04b91a1/d049bd1e-7fb5-46f7-ba29-5b058223a1dd-641","language":"ko-KO","phone_number":"","status":"active","role_id":"0","user_created_at":"2020-07-12T04:17:16Z"}]}
     * **/
    @GetMapping("/users")
    public String getUsers(@RequestParam String userId, Model model) {
        System.out.println("userId :" + userId);
        String userUrl = zoomApiBaseUrl + "/users/" + userId;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userUrl,
                    HttpMethod.GET,
                    tokenRequestEntity,
                    String.class);

            System.out.println(response.getBody());
            model.addAttribute("users", response.getBody());

        } catch (HttpClientErrorException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

    /**사용자의 모든 스케줄러를 나열합니다. <br>
     * <a href="https://developers.zoom.us/docs/api/rest/reference/zoom-api/methods/#operation/meeting">...</a>
     * **/
    @GetMapping("/schedulers")
    public String getScheduledMeetingIdZoomApi(@RequestParam String userId, Model model) {
        System.out.println("userId :" + userId);
        String meetingUrl = zoomApiBaseUrl + "/users/" + userId + "/schedulers";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    meetingUrl,
                    HttpMethod.GET,
                    tokenRequestEntity,
                    String.class);
            System.out.println(response.getBody());
            model.addAttribute("schedulers", response.getBody());

        }catch (HttpClientErrorException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }

    /**주어진 회의의 세부정보를 검색합니다. <br>
     * <a href="https://developers.zoom.us/docs/api/rest/reference/zoom-api/methods/#operation/meeting">...</a>
     * **/
    @GetMapping("/meetings")
    public String getMeetingIdZoomApi(@RequestParam String userId, Model model) {
        System.out.println("userId :" + userId);
        String meetingUrl = zoomApiBaseUrl + "/users/" + userId + "/meetings";

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    meetingUrl,
                    HttpMethod.GET,
                    tokenRequestEntity,
                    String.class);
            System.out.println(response.getBody());

            model.addAttribute("meetings", response.getBody());
        }catch (HttpClientErrorException e) {
            model.addAttribute("messages",e.toString());
        }

        setModelObject(model);
        return "zoom";
    }
        /*
        {
          "assistant_id": "kFFvsJc-Q1OSxaJQLvaa_A",
          "host_email": "jchill@example.com",
          "host_id": "30R7kT7bTIKSNUFEuH_Qlg",
          "id": 97763643886,
          "uuid": "aDYlohsHRtCd4ii1uC2+hA==",
          "agenda": "My Meeting",
          "created_at": "2022-03-25T07:29:29Z",
          "duration": 60,
          "encrypted_password": "8pEkRweVXPV3Ob2KJYgFTRlDtl1gSn.1",
          "pstn_password": "123456",
          "h323_password": "123456",
          "join_url": "https://example.com/j/11111",
          "chat_join_url": "https://example.com/launch/jc/11111",
          "occurrences": [
            {
              "duration": 60,
              "occurrence_id": "1648194360000",
              "start_time": "2022-03-25T07:46:00Z",
              "status": "available"
            }
          ],
          "password": "123456",
          "pmi": "97891943927",
          "pre_schedule": false,
          "recurrence": {
            "end_date_time": "2022-04-02T15:59:00Z",
            "end_times": 7,
            "monthly_day": 1,
            "monthly_week": 1,
            "monthly_week_day": 1,
            "repeat_interval": 1,
            "type": 1,
            "weekly_days": "1"
          },
          "settings": {
            "allow_multiple_devices": true,
            "alternative_hosts": "jchill@example.com;thill@example.com",
            "alternative_hosts_email_notification": true,
            "alternative_host_update_polls": true,
            "approval_type": 0,
            "approved_or_denied_countries_or_regions": {
              "approved_list": [
                "CX"
              ],
              "denied_list": [
                "CA"
              ],
              "enable": true,
              "method": "approve"
            },
            "audio": "telephony",
            "audio_conference_info": "test",
            "authentication_domains": "example.com",
            "authentication_exception": [
              {
                "email": "jchill@example.com",
                "name": "Jill Chill",
                "join_url": "https://example.com/s/11111"
              }
            ],
            "authentication_name": "Sign in to Zoom",
            "authentication_option": "signIn_D8cJuqWVQ623CI4Q8yQK0Q",
            "auto_recording": "cloud",
            "breakout_room": {
              "enable": true,
              "rooms": [
                {
                  "name": "room1",
                  "participants": [
                    "jchill@example.com"
                  ]
                }
              ]
            },
            "calendar_type": 1,
            "close_registration": false,
            "contact_email": "jchill@example.com",
            "contact_name": "Jill Chill",
            "custom_keys": [
              {
                "key": "key1",
                "value": "value1"
              }
            ],
            "email_notification": true,
            "encryption_type": "enhanced_encryption",
            "focus_mode": true,
            "global_dial_in_countries": [
              "US"
            ],
            "global_dial_in_numbers": [
              {
                "city": "New York",
                "country": "US",
                "country_name": "US",
                "number": "+1 1000200200",
                "type": "toll"
              }
            ],
            "host_video": true,
            "jbh_time": 0,
            "join_before_host": true,
            "language_interpretation": {
              "enable": true,
              "interpreters": [
                {
                  "email": "interpreter@example.com",
                  "languages": "US,FR"
                }
              ]
            },
            "sign_language_interpretation": {
              "enable": true,
              "interpreters": [
                {
                  "email": "interpreter@example.com",
                  "sign_language": "American"
                }
              ]
            },
            "meeting_authentication": true,
            "mute_upon_entry": false,
            "participant_video": false,
            "private_meeting": false,
            "registrants_confirmation_email": true,
            "registrants_email_notification": true,
            "registration_type": 1,
            "show_share_button": true,
            "use_pmi": false,
            "waiting_room": false,
            "watermark": false,
            "host_save_video_order": true,
            "internal_meeting": false,
            "meeting_invitees": [
              {
                "email": "jchill@example.com",
                "internal_user": false
              }
            ],
            "continuous_meeting_chat": {
              "enable": true,
              "auto_add_invited_external_users": true,
              "auto_add_meeting_participants": true,
              "channel_id": "cabc1234567defghijkl01234"
            },
            "participant_focused_meeting": false,
            "push_change_to_calendar": false,
            "resources": [
              {
                "resource_type": "whiteboard",
                "resource_id": "X4Hy02w3QUOdskKofgb9Jg",
                "permission_level": "editor"
              }
            ],
            "auto_start_meeting_summary": false,
            "auto_start_ai_companion_questions": false,
            "device_testing": false
          },
          "start_time": "2022-03-25T07:29:29Z",
          "start_url": "https://example.com/s/11111",
          "status": "waiting",
          "timezone": "America/Los_Angeles",
          "topic": "My Meeting",
          "tracking_fields": [
            {
              "field": "field1",
              "value": "value1",
              "visible": true
            }
          ],
          "type": 2,
          "dynamic_host_key": "123456"
        }
    * */

}
