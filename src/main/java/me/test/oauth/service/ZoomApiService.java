package me.test.oauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.common.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** ZoomApi 호출하는 하나의 인스턴스만 생성해서 스프링이 관리함, 여러 컨트롤러에서 참조로 공통 사용함. Zoom 사이트에 API 요청하고, 받은 데이터 String Json 형태로 반환. **/
@Service
@PropertySource("classpath:zoom.properties")
@Slf4j
@RequiredArgsConstructor
public class ZoomApiService {

    /** HTTP 통신을 위한 도구로 REST full API 웹 서비스와의 상호작용을 쉽게 외부 도메인에서 데이터를 가져오거나 전송할 때 사용되는 스프링 프레임워크의 클래스. PATCH 지원을 위해 스프링에 등록해둔 빈을 주입받아야함. 직접생싱시 PATCH 사용불가**/
    private final RestTemplate restTemplate;
    /** 권한요청용 base url **/
    @Value("${zoom.oauth.endpoint}")
    private String zoomOauthEndpoint;
    /** api 호출용 base url **/
    @Value("${zoom.api.base.url}")
    String zoomApiBaseUrl;

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

    //    post 요청용
    private HttpHeaders postHeaders = new HttpHeaders();

    private final ObjectMapper objectMapper = JsonUtil.getObjectMapper();

    //    생성자 실행시 서버 권한 생성
    @PostConstruct
    public void init() {
        try {
            accountCredentials(null);
        }catch (Exception e) {
            log.info("accountCredentials model is null, but get token success : {}", e.getMessage());
        }
    }

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

        postHeaders = new HttpHeaders();
        postHeaders.setContentType(MediaType.APPLICATION_JSON);
        postHeaders.setBearerAuth(token);
    }

    public void setModelObject(Model model) {
        model.addAttribute("clientToken", CLIENT_CREDENTIALS_ACCESS_TOKEN !=null? CLIENT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("accountToken", ACCOUNT_CREDENTIALS_ACCESS_TOKEN !=null? ACCOUNT_CREDENTIALS_ACCESS_TOKEN : "");
        model.addAttribute("authorizationCode",  AUTHORIZATION_CODE !=null? AUTHORIZATION_CODE : "");
        model.addAttribute("accessToken", USER_ACCESS_TOKEN !=null? USER_ACCESS_TOKEN : "");
        model.addAttribute("refreshToken", USER_REFRESH_TOKEN !=null? USER_REFRESH_TOKEN : "");
        model.addAttribute("isSuccess", CLIENT_CREDENTIALS_ACCESS_TOKEN!=null || ACCOUNT_CREDENTIALS_ACCESS_TOKEN!=null || USER_ACCESS_TOKEN!=null);
    }

    /** 모든 정보 초기화 **/
    public void resetToken(Model model) {
        CLIENT_CREDENTIALS_ACCESS_TOKEN = null;
        ACCOUNT_CREDENTIALS_ACCESS_TOKEN = null;
        AUTHORIZATION_CODE = null;
        USER_ACCESS_TOKEN = null;
        USER_REFRESH_TOKEN = null;
        setModelObject(model);
    }

    /** 애플리케이션 자체가 API 에 액세스해야 할 때
     * clientCredentials > authorize : 리다이렉트 > get/auth > token
     * 성공적인 응답
     * {"access_token":"~","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"<a href="https://api.zoom.us">...</a>"}
     *
     * 성공시, 쿼리 매개변수를 사용하여 인증 코드를 요청합니다.
     * <br>redirect_uri 권한이 부여된 경우 Zoom 은 코드 쿼리 매개변수에 권한 부여 코드를 사용하여 사용자를 리디렉션합니다 .
     * <br>https://{{ZOOM_REDIRECT_URI}}?code=~
     **/
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
            log.debug("[test] client - response: " + response.getStatusCode() + response.getBody());

            // 토큰 저장
            String responseBody = response.getBody();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(responseBody);
            CLIENT_CREDENTIALS_ACCESS_TOKEN = jsonNode.get("access_token").asText();


            UriComponentsBuilder authBuilder = UriComponentsBuilder.fromHttpUrl(zoomOauthEndpoint + "/authorize")
                    .queryParam("response_type", "code")
                    .queryParam("redirect_uri", ZOOM_REDIRECT_URI)
                    .queryParam("client_id", zoomClientId);
            String redirectUri = authBuilder.build().toUriString();

            try{
                redirectResponse.sendRedirect(redirectUri);
                log.debug("[test] authorize - sendRedirect");
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

            log.debug("[test]" + messages);
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
    public String getZoomApiAuth(HttpServletRequest req, @RequestParam String code, Model model) {
        log.debug("redirect_uri: "+ "zoom/get/auth");

        if(req.getParameter("code") != null) {
            AUTHORIZATION_CODE = req.getParameter("code");

            //성공적으로 code 를 받으면 token 요청을 한다
            String tokenUrl = zoomOauthEndpoint + "/token";

            // 바디생성
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", "authorization_code");
            requestBody.add("code", AUTHORIZATION_CODE);
            requestBody.add("redirect_uri", ZOOM_REDIRECT_URI);
            HttpHeaders clientHeaders = getClientHeaders();
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, clientHeaders);

            try{
                ResponseEntity<String> response = restTemplate.exchange(
                        tokenUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                log.debug("token - response: " + response.getStatusCode() + response.getBody());

                // 토큰 저장
                String responseBody  = response.getBody();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);
                USER_ACCESS_TOKEN = jsonNode.get("access_token").asText();
                USER_REFRESH_TOKEN = jsonNode.get("refresh_token").asText();
                setTokenHeaders(USER_ACCESS_TOKEN);
                log.debug("access_token : " + USER_ACCESS_TOKEN);

            } catch (HttpClientErrorException e) {
                String messages = "token - error: " +
                        e.getStatusCode().value() +
                        e.getResponseBodyAsString() +
                        "clientHeaders \n" +
                        clientHeaders +
                        "requestBody \n" +
                        requestBody;

                log.debug(messages);
                model.addAttribute("messages",messages);
            } catch (JsonProcessingException e) {
                model.addAttribute("messages",e.toString());
            }
        }else {
            model.addAttribute("messages", "Authorize Code is null");
        }
        log.debug("code: %s", code);

        setModelObject(model);
        return "zoom";
    }

    /** 애플리케이션 소유자의 액세스 토큰입니다. 서버에서 API 에 액세스해야 할 때 (사용자 인증이 필요하지 않은 경우)
     * 새로 고침 토큰이 없습니다. 한 시간 후에 만료됩니다.
     * 계정 관리자는 이러한 앱 유형을 빌드하는 개발자가 사용할 수 있는 범위를 승인합니다.
     * 성공적인 응답
     * {"access_token":"~","token_type":"bearer","expires_in":3600,"scope":"marketplace:delete:event_subscription marketplace:read:list_event_subscriptions marketplace:update:event_subscription marketplace:write:event_subscription marketplace:write:websocket_connection","api_url":"<a href="https://api.zoom.us">...</a>"}
     * **/
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

            log.debug("account_credentials - response: " + response.getStatusCode() + response.getBody());

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

    /** 토근을 가지고 get 요청 API 호출 JSON 반환 **/
    /** 전역변수 tokenRequestEntity 를 사용하여 GET 요청을 보냅니다.
     *
     * <br> 요청헤더에 엑세스 토큰이 필요합니다.
     * <br> -H "Authorization: Bearer USER_ACCESS_TOKEN"
     * <br> -H "Content-Type: application/x-www-form-urlencoded"
     * **/
    public String getApi(String getUrl) {
        String profix = "fail";
        // 요청 가능여부 검증
        if(tokenHeaders == null) {
            log.debug("tokenHeaders is null. fail to get api %s\n", zoomApiBaseUrl + getUrl);
            return profix + "tokenHeaders is null. fail to get api %s\n"+ zoomApiBaseUrl + getUrl;
        }

        try {
            HttpEntity<Map<String, Object>> tokenRequestEntity = new HttpEntity<>(tokenHeaders);
            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    zoomApiBaseUrl + getUrl,
                    HttpMethod.GET,
                    tokenRequestEntity,
                    String.class
            );

            // 응답 바디 JSON 형로 출력 : 4개의 공백으로 들여쓰기
            ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            Object jsonObject = objectMapper.readValue(response.getBody(), Object.class);
            String prettyJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

            return prettyJsonString;

        } catch (Exception e) {
            return profix + e.toString();
        }
    }

    /** 토근을 가지고 post 요청 API 호출 JSON 반환 **/
    @Deprecated
    public ResponseEntity<String> putApi(String url, Map<String, Object> bodyMap) {
        log.info("[test]postApi : {}", bodyMap);
        String profix = "fail";
        String postUrl = zoomApiBaseUrl + url;
        // 요청 가능여부 검증
        if(postHeaders == null) {
            log.debug("tokenHeaders is null. fail to get api %s\n", postUrl);
            return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(profix + "tokenHeaders is null. fail to get api %s\n"+ postUrl);
        }

        try {
            HttpEntity<Map<String, Object>> tokenRequestEntity = new HttpEntity<>(bodyMap,postHeaders);
            log.info("[test]postApi : {}", tokenRequestEntity);
            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    postUrl,
                    HttpMethod.PUT,
                    tokenRequestEntity,
                    String.class
            );

            return response;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(profix + e.toString());
        }
    }

    /** 토근을 가지고 post 요청 API 호출 JSON 반환 **/
    @Deprecated
    public ResponseEntity<String> postApi(String url, Map<String, Object> bodyMap) {
        log.info("[test]postApi : {}", bodyMap);
        String profix = "fail";
        String postUrl = zoomApiBaseUrl + url;
        // 요청 가능여부 검증
        if(postHeaders == null) {
            log.debug("tokenHeaders is null. fail to get api %s\n", postUrl);
            return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(profix + "tokenHeaders is null. fail to get api %s\n"+ postUrl);
        }

        try {
            HttpEntity<Map<String, Object>> tokenRequestEntity = new HttpEntity<>(bodyMap,postHeaders);
            log.info("[test]postApi : {}", tokenRequestEntity);
            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    postUrl,
                    HttpMethod.POST,
                    tokenRequestEntity,
                    String.class
            );

            return response;

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(profix + e.toString());
        }
    }

    /** 토근을 가지고 get 요청 API 호출 JSON 반환 **/
    /** 전역변수 tokenRequestEntity 를 사용하여 GET 요청을 보냅니다.
     *
     * <br> 요청헤더에 엑세스 토큰이 필요합니다.
     * <br> -H "Authorization: Bearer USER_ACCESS_TOKEN"
     * <br> -H "Content-Type: application/x-www-form-urlencoded"
     * **/
    @Deprecated
    public ResponseEntity<String> deleteApi(String getUrl) {
        String profix = "fail";
        // 요청 가능여부 검증
        if(tokenHeaders == null) {
            log.debug("tokenHeaders is null. fail to get api %s\n", zoomApiBaseUrl + getUrl);
            //return profix + "tokenHeaders is null. fail to get api %s\n"+ zoomApiBaseUrl + getUrl;
            return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(profix + "tokenHeaders is null. fail to get api %s\n"+ zoomApiBaseUrl + getUrl);
        }

        try {

            HttpEntity<Map<String, Object>> tokenRequestEntity = new HttpEntity<>(tokenHeaders);
            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    zoomApiBaseUrl + getUrl,
                    HttpMethod.DELETE,
                    tokenRequestEntity,
                    String.class
            );
            return response;

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(profix + e.toString());
        }
    }

    /** 공통 API 호출 함수 ResponseEntity<String> 반환 **/
    public ResponseEntity<String> api(String url, HttpMethod httpMethod, Map<String, Object> bodyMap) {
        HttpEntity<Map<String, Object>> tokenRequestEntity;
        String errorMessage = "tokenHeaders is null. fail to api" + httpMethod.toString() + zoomApiBaseUrl + url;

        try {
            if(httpMethod.equals(HttpMethod.GET) || httpMethod.equals(HttpMethod.DELETE)) {
                if(tokenHeaders == null) {
                    log.warn(errorMessage);
                    return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(errorMessage);
                }
                tokenRequestEntity = new HttpEntity<>(tokenHeaders);
            }else {
                if(postHeaders == null) {
                    log.warn(errorMessage);
                    return ResponseEntity.status(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED).body(errorMessage);
                }
                tokenRequestEntity = new HttpEntity<>(bodyMap, postHeaders);
            }

            // REST API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    zoomApiBaseUrl + url,
                    httpMethod,
                    tokenRequestEntity,
                    String.class
            );
            return response;

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString());
        }
    }



    public String pasingJson(String body) throws JsonProcessingException {
        // 응답 바디 JSON 형로 출력 : 4개의 공백으로 들여쓰기
        Object jsonObject = objectMapper.readValue(body, Object.class);
        String prettyJsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        return prettyJsonString;
    }

}
