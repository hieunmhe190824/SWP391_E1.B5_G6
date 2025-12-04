package com.group3.carrental.model;

import java.io.Serializable;

public class User implements Serializable{
    private int id;
    private String username;
    private String email;
    private String password;
    private String fullname;
    private String phone;
    private String url;

    public User() {

    }
    public User(int id, String username, String email, String password, String fullname, String phone, String url) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullname = fullname;
        this.phone = phone;
        this.url = url;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
}
