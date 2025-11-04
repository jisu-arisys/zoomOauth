package me.test.oauth.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import me.test.oauth.entity.CustomUserDetails;
import me.test.oauth.entity.User;
import me.test.oauth.jwt.JwtUtils;
import me.test.oauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** B-1. security 의 formLogin 사용하지 않고, rest 방식 중에서도 수동으로 직접 처리하기위한 컨트롤러 **/
@RestController
@RequestMapping("/auth") //rewite 로 "/api" path 제거
@Slf4j
public class SecurityController {

    private final UserRepository userRepository;

    @GetMapping("/user")
    public static String getLoginUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            // 인증된 사용자가 있을 경우
            // 인증된 사용자 정보 확인
            return ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
        }
        return null;
    }



    @GetMapping(value = "/check/login")
    public ResponseEntity<String> checkLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            // 인증된 사용자가 있을 경우
            System.out.println(authentication.getPrincipal());
            return ResponseEntity.ok("login");
        }else {
            System.out.println("checkLogin");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    private final AuthenticationManager authenticationManager;
    public SecurityController(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    private Authentication setAuthentication(User loginUser, HttpSession session) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginUser.getUsername(),
                        loginUser.getPassword()
                )
        );

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        return authentication;
    }

    /** RequestBody (application/json) 사용 (JSON 방식)하는 loginProcessingUrl api 를 직접 구현
     * UsernamePasswordAuthenticationFilter 미사용 setAuthentication 로 직접구현
     * RequestBody 로 JSON 객체를 전달하여 처리. **/
    @PostMapping("/login")
    @Deprecated
    public ResponseEntity<?> login(@RequestBody User loginUser, HttpSession session) { //로그인요청 처리
        log.warn("/auth/login 컨트롤러 호출됨");
        Optional<User> foundUser = userRepository.findByEmail(loginUser.getUsername());
        if(foundUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }else {
            Authentication authentication = setAuthentication(loginUser, session); // spring security 세션에 사용자 로그인처리
            // JWT 발급 (JWT 사용 시) 응답값에 map 으로 감싸서 전달.
			String token = JwtUtils.generateToken(authentication);
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", foundUser.get());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map> me(Authentication authentication) {
        log.debug("[test] /auth/me");
        if (authentication == null) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
        org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String username = user.getUsername();
        User currentUser = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        User userDto = new CustomUserDetails(currentUser).getUserForJWT();

        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "user", userDto
        ));
    }
}
