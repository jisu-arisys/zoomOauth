
## filterChain 에서 HttpSecurity 를 설정할때,  

Spring Security 6부터는 권한 부여 규칙에서 요구하지 않는 한(denyAll, permitAll) 세션에 ping을 보내지 않습니다. 
세션에서 인증을 가져오지 않음으로써 성능 영향이 해결되었으므로, 가능한 모든 요청에 권한을 정의해 주는 것이 좋습니다.

권한 설정은 나열된 순서대로 처리하므로 첫 번째 일치 항목에만 적용합니다.

스프링은 모든 디스패처 유형을 관리하므로 사용자에 의한 요청 request 외에도 시스템 처리에 의한 forwards, errors, includes 에 대한 권한도 정의해야한다.
🥽DispatcherType : 디스패처 유형
* REQUEST : 클라이언트에서 온 요청
* ERROR : 에러에 해당하는 요청
* FORWARD : 서블릿에서 다른 서블릿이나 JSP를 호출할 때
* INCLUDE : 서블릿에서 다른 서블릿이나 JSP의 결과를 포함할 때
* ASYNC : 서블릿 비동기 호출

권한을 정의할 대상을 선택한다.
* dispatcherTypeMatchers(DispatcherType...) : 디스패치 유형에 따라 정의함. 가변인자를 받음.
* requestMatchers(HttpMethod) : Http 유형에 따라 권한을 다르게 정의한다. ex. GET, POST 등.
* requestMatchers(HttpMethod) : Http 유형에 따라 권한을 다르게 정의한다. ex. GET, POST 등.
* requestMatchers(String...) : 일치하는 컨트롤러 경로의 요청에 대해서 정의함. 가변인자나 정규표현식을 통해 다중 정의할 수도 있다. 
  * ("/path", "/api") : /path 경로와 /api 경로에 대해 권한을 일괄 설정함.
  * ("/path/**") : /path 하위의 모든 경로에 대해 권한을 일괄 설정함.
  * ("/resource/[A-Za-z0-9]+") : /resource 하위의 1개 이상의 영문과 숫자로 이루어진 모든 경로에 대해 권한을 일괄 설정함.
* anyRequest() : 모든 컨트롤러 경로의 요청에 대해서 정의함.

권한 부여 옵션
* .denyAll() : 인증후 어떠한 상황에서도 접근을 거부. 로그인 후 403 에러 발생. 더이상 사용하지 않는 페이지에 사용됨.
* .permitAll() : 인증불필요
* .authenticated() : 인증필요
* .hasRole(String) : 지정된 역할을 가진 사용자만 인증 후 접근가능
* .hasAnyRole(String...) : 지정된 하나 이상의 역할을 가진 사용자만 인증 후 접근가능
* .hasAuthority(String) : 지정된 권한을 가진 사용자만 인증 후 접근가능
* .hasAnyAuthority(String...) : 지정된 하나 이상의 권한을 가진 사용자만 인증 후 접근가능
* .access(AuthorizationManager) : 사용자 정의에 의해 접근여부를 결정 ex. allOf(hasAuthority("db"), hasRole("ADMIN"))

역할과 권한은 접두사(prefix)의 차이.
* Role : "USER", "ADMIN", "MANAGER" ... : 데이터베이스에 사용자 권한 컬럼에서 'ROLE_USER' 또는 'ROLE_ADMIN'이 있는지 조회함. 'ROLE_' 이 포함되어 있어야 한다.
* Authority : "READ_PRIVILEGES", "WRITE_PRIVILEGES", "DELETE_PRIVILEGES" ... : 사용자 권한에서 'READ_PRIVILEGES' 또는 'WRITE_PRIVILEGES'이 있는지 조회함


## 서블릿 경로가 다른 경우,

보통은 서블릿 경로(spring.mvc.servlet.path)를 기본값인 루트경로 '/'로 지정한다.
그러나 다르게 지정한다면 아래의 설정이 필요하다. 서블릿 경로와 컨트롤러 경로를 분할 해 정의해주어야 한다.

* MvcRequestMatcher.Builder(HandlerMappingIntrospector).servletPath(String);
* MvcRequestMatcher.Builder.pattern("/my/controller/**"))

```java

@Bean
MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
	return new MvcRequestMatcher.Builder(introspector).servletPath("/spring-mvc");
}

@Bean
SecurityFilterChain appEndpoints(HttpSecurity http, MvcRequestMatcher.Builder mvc) {
    http
    .authorizeHttpRequests((authorize) -> authorize
    .requestMatchers(mvc.pattern("/my/controller/**")).hasAuthority("controller")
    .anyRequest().authenticated()
    );
    
    return http.build();
}
        
```

## 체이닝 방식 지양

메소드의 옵션값을 체이닝 사용함으로 혼선을 주던 과거 방식을 지양하고
메서드의 매개변수에 옵션과 관련한 람다함수를 포함하는 방식으로 내부 메소드가 변경됨. 
과거 방식은 IDE 에서 문법상 오류로 보고 에러를 발생시킴.

예제 csrf 비활성화

```
http
    //.csrf().disable() // 과거방식
    .csrf( csrf -> csrf.disable()) // 변경된 방식
```

```java
//관련코드
@Deprecated( since = "6.1", forRemoval = true )
public CsrfConfigurer<HttpSecurity> csrf() throws Exception {
        ApplicationContext context = this.getContext();
        return (CsrfConfigurer)this.getOrApply(new CsrfConfigurer(context));
        }

public HttpSecurity csrf(Customizer<CsrfConfigurer<HttpSecurity>> csrfCustomizer) throws Exception {
        ApplicationContext context = this.getContext();
        csrfCustomizer.customize((CsrfConfigurer)this.getOrApply(new CsrfConfigurer(context)));
        return this;
        }
```

## 데이터베이스 h2 를 사용하기위한 설정

h2-console은 frame 태그를 통해 구현한다.
X-Frame-Options가 기본값 DENY로 설정되어 있으면, 해당 도메인의 페이지는 어디에서도  <frame> 혹은 <iframe>태그로 로드될 수가 없게 된다.

아래 경로로 http 요청을 보낸다.
`http://localhost:8080/h2-console/header.jsp?jsessionid=0340fe8bf5d09068c343660456ea676e`
