package com.boda.bousers.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVerifyResponse {
    private String name;
    private boolean alreadyRegistered;
}
