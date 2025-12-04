package com.carrental.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "customer/home";
    }

    @GetMapping("/about")
    public String about() {
        return "customer/about";
    }
}
