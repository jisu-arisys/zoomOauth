package me.test.oauth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private final JwtUtils jwtUtils;

    public JwtAuthorizationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        boolean notFilteringPath = "/.well-known/appspecific/com.chrome.devtools.json".equals(path);
        //logger.debug("shouldNotFilter: {}", path);
        return notFilteringPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (shouldNotFilter(request)) {filterChain.doFilter(request, response);}
        try {
            String token = getTokenFromRequest(request);
            String requestURI = request.getRequestURI();
//            logger.debug("doFilterInternal: {}, {}",token);

            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                // JWT가 유효하다면 사용자 정보를 SecurityContext에 저장
                Authentication authentication = jwtUtils.getAuthoritiesFromToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 세션 생성
                HttpSession session = request.getSession(true); // 세션이 없으면 생성
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

                logger.debug("Security Context 에 {} 인증정보 저장 완료. 요청한 uri: {}", authentication.getName(), requestURI);
            }
        } catch (Exception e) {
            logger.error("JWT 검증 실패: {}", e.getMessage());
        } finally {
            filterChain.doFilter(request, response); // 다음 필터로 전달
        }

    }

    /** 요청헤더 에서 token 추출 **/
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtUtils.AUTHORIZATION_HEADER);
        //logger.debug("getTokenFromRequest : {}", bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtUtils.BEARER_TOKEN_PREFIX)) {
            return bearerToken.substring(7); // "Bearer " 이후의 토큰 값 추출
        }
        return null;
    }
}
