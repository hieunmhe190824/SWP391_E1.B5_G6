package com.group3.carrental.mapper;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group3.carrental.model.User;

@RestController
@RequestMapping(path = "/api/v1/Users")
public class APIController {
    @GetMapping("")
    //request at http://localhost:8080/api/v1/Users
    // public String getMethodName(@RequestParam String param) {
    //     return new String();
    // }
    List<User> getAllUsers() {
        return List.of(
            new User(1, "alexrobert", "alexrobert@gmail.com", "alexbozo", "Alex Robert", "0981304469", "www.facebook.com")

        );
    }
}
