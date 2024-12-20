package me.test.oauth.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import static org.springframework.security.config.Customizer.withDefaults;

//spring boot의 주요원칙인 컴포넌트 기반 설정을 유지하기위해 extands WebSecurityConfigurerAdapter 구조는 deprecated 하고, @Bean 방식을 사용한다.
//Spring Security 5.7 이후로 @EnableWebSecurity 는 더 이상 필수적이지 않다.
@Configuration
public class SecurityConfig{
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // token을 사용하는 방식이기 때문에 csrf를 disable합니다.
                // 2. H2 Console은 CSRF 보호를 비활성화해야 정상 작동합니다.
                .csrf(AbstractHttpConfigurer::disable)
                // 3. enable h2-console
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )

                //1. 요청 수준에서 권한을 모델링함.
                .authorizeHttpRequests((authorize) -> authorize
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll() // request 외 디스패처 유형 인증 불필요
                        .requestMatchers("/h2-console/**").permitAll() // 브라우저에서 사용하는 DB인 h2에 콘솔로 접근할 때 인증 불필요 (기본 경로는 /h2-console)
                        .requestMatchers("/favicon.ico").permitAll() // 페이지를 대표하는 아이콘
                        .requestMatchers("/zoom/**").permitAll()// 지정 엔드포인트 인증 불필요
                        .requestMatchers("/hello").permitAll()// 지정 엔드포인트 인증 불필요
                        .requestMatchers("/").permitAll()// 지정 엔드포인트 인증 불필요
//                        .requestMatchers("/user/home").permitAll()// 지정 엔드포인트 인증 불필요
//                        .requestMatchers("/db/read").hasRole("USER")// 권한 인증이 필요
//                        .requestMatchers("/db/insert").access(allOf(hasAuthority("WRITE"), hasRole("USER")))// 사용자 정의한 권한 인증이 필요
//                        .requestMatchers("/db/delete").access(allOf(hasAuthority("DELETE"), hasRole("ADMIN")))// 사용자 정의한 권한 인증이 필요
                        .anyRequest().authenticated() // 그외 모든 엔드포인트 인증필요
                )

                //시큐리티 제공 로그인페이지을 위한 최소한의 명시적 Java 구성
                .formLogin(withDefaults());

        return http.build();
    }

}
