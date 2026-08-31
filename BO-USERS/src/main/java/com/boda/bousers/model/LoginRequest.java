package com.boda.bousers.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
