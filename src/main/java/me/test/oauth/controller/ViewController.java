package me.test.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 정적 html 화면출력을 위한 컨트롤러 **/
@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/list")
    public String list() {
        return "users";
    }
}
