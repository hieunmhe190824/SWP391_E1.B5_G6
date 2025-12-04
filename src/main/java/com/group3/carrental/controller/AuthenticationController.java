package com.group3.carrental.controller;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group3.carrental.dal.UserDAO;
import com.group3.carrental.model.Email;
import com.group3.carrental.model.User;
import com.group3.carrental.security.Encryptor;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthenticationController {

    UserDAO uDao = new UserDAO();

    @PostMapping("/login")
    public String login(
        @RequestParam("username") String username,
        @RequestParam("password") String password,
        HttpSession session,
        RedirectAttributes ra
        ) throws NoSuchAlgorithmException{
            Encryptor encrypt = new Encryptor();
            User userDB = uDao.findUser(username, encrypt.encryptString(password), null);
            if (userDB != null) {
                session.setAttribute("currentUser", userDB.getFullname());
                return "redirect:/home";
            } else {
                ra.addFlashAttribute("remainUser", username);
                return "redirect:/login";
            }
    }

    @PostMapping("/register")
    public String register(
        @RequestParam("username") String username,
        @RequestParam("password") String password,
        @RequestParam("fname") String firstname,
        @RequestParam("lname") String lastname,
        @RequestParam("email") String email,
        HttpSession session,
        RedirectAttributes ra
        ) throws NoSuchAlgorithmException {
            Encryptor encrypt = new Encryptor();
            password = encrypt.encryptString(password);
            uDao.createUserinDatabase(username, password, firstname, lastname, email);
        return "redirect:/login";
    }

    @PostMapping("/forgot_password")
    public String forgot_password(
        @RequestParam("username_email") String input,
        HttpSession session,
        RedirectAttributes ra
        ) throws MessagingException, UnsupportedEncodingException {
            User user = new User(); //assume creating new user = null cause of empty default constructor
            if (input.contains("@")) {
                //find email
                user = uDao.findUser(null, null, input);
            } else {
                //find username
                user = uDao.findUser(input, null, null);
            }
            if (user != null) {
                Email.sendEmail(user.getEmail(), user.getFullname());
                ra.addFlashAttribute("success", "We've sent you an email to verify and change your password!");
                //create error msg: sent!
            } else {
                ra.addFlashAttribute("remainUser", input);
                ra.addFlashAttribute("error", "Username or email not exist!");
            }
        return "redirect:/forgot_password";//put this in the above if condition
    }
}
