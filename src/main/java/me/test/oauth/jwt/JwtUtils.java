package me.test.oauth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final static String AUTHORITIES_KEY = "auth ";
    private static String SECRET_KEY; //보안처리해야함
    private static long EXPIRATION_TIME ; // 24시간
    public String AUTHORIZATION_HEADER;
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static Key key;

    public JwtUtils(@Value("${jwt.secret}") String secretKey, @Value("${jwt.token-validity-in-seconds}") long expirationTime, @Value("${jwt.header}") String header) {
        SECRET_KEY = secretKey;
        EXPIRATION_TIME = expirationTime;
        AUTHORIZATION_HEADER = header;
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    private static Date createDateFromMillis(long millis) {
        return new Date(millis);
    }
    public static String generateToken(Authentication authentication) {
        String authoriString = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        long currentTimeMillis = System.currentTimeMillis();
        Date issuedAt = createDateFromMillis(currentTimeMillis);
        Date expiration = createDateFromMillis(currentTimeMillis + EXPIRATION_TIME);
        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authoriString)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, key)
                .compact();
    }

    public Authentication getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody(); // payload 데이터 가져오기
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                                                            .map(SimpleGrantedAuthority::new)
                                                            .collect(Collectors.toList());
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public static boolean validateToken(String token) {
        try {
            // 토큰의 유효성을 확인하며 payload(claims) 파싱
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            logger.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            logger.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            logger.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            logger.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}