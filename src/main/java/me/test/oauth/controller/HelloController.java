package me.test.oauth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HelloController {

    //HttpStatus, HttpHeaders, HttpBody 를 갖는 ResponseEntity 객체 반환, 이때 body 타입을 String 으로 명시함. why? 직관적, 자동형변환
    //작은 프로젝트라면 와일드카드 사용이 생산성에 도움될 수도 있음. 큰 프로젝트나 api 라면 객체를 명시하는 것이 좋음.
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        System.out.println("helloController");

        //new 를 쓰는 생성자패턴 보다는 메소드 체이닝으로 된 빌더패턴을 권장한다. why? 직관적, 유지보수 용이
        return ResponseEntity.ok("hello");
    }
}
