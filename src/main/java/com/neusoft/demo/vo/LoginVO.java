package com.neusoft.demo.vo;

public class LoginVO {

    private Long id;
    private String name;
    private String role;
    private String token;
    private String title;        // ← 新增 职称
    private String username;     // ← 新增 账号

    public LoginVO() {
    }

    public LoginVO(Long id, String name, String role, String token) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.token = token;
    }

    // ── getter/setter ──
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}