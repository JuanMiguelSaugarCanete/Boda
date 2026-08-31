package com.boda.bousers.model;

import lombok.Data;

import java.util.List;

@Data
public class RegisterCompleteRequest {
    private String phone;
    private String generalPassword;
    private String name;
    private String email;
    private String password;
    private String attendance;
    private List<String> allergens;
    private String diet;
    private String dietNotes;
    private String imgProfile;
}
