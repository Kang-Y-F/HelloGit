package com.neusoft.demo.dto;

import lombok.Data;

@Data
public class LoginDTO {

    private String phone;

    public LoginDTO() {
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}