package com.boda.bousers.controllers;

import com.boda.bousers.model.LoginRequest;
import com.boda.bousers.model.LoginResponse;
import com.boda.bousers.model.RegisterCompleteRequest;
import com.boda.bousers.model.RegisterVerifyRequest;
import com.boda.bousers.model.RegisterVerifyResponse;
import com.boda.bousers.model.SessionResponse;
import com.boda.bousers.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> loginGuest(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyRegistration(@RequestBody RegisterVerifyRequest request) {
        try {
            RegisterVerifyResponse response = authService.verifyRegistration(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            SessionResponse response = authService.getSession(authHeader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register/complete")
    public ResponseEntity<?> completeRegistration(@RequestBody RegisterCompleteRequest request) {
        try {
            LoginResponse response = authService.completeRegistration(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
