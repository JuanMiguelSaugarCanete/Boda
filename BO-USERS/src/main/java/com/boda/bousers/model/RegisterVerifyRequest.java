package com.boda.bousers.model;

import lombok.Data;

@Data
public class RegisterVerifyRequest {
    private String phone;
    private String generalPassword;
}
