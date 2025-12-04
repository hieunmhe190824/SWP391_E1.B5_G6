package com.group3.carrental.mapper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class MapperController {

    @GetMapping({ "/", "/home" })
    public String home(HttpSession session, Model model) {
        Object user = session.getAttribute("currentUser");
        model.addAttribute("userSession", user);
        return "home";
    }
    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/home";
    }

    @GetMapping("/forgot_password")
    public String forgot_password(HttpSession session) {
        session.invalidate();
        return "forgot_password";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("change_password")
    public String change_password(HttpSession session, Model model) {
        Object token = session.getAttribute("getMailToken");
        model.addAttribute("token", token);
        return "change_password";
    }
}