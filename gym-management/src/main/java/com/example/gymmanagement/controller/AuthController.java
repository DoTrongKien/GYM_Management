package com.example.gymmanagement.controller;

import com.example.gymmanagement.dto.AuthResponse;
import com.example.gymmanagement.dto.LoginRequest;
import com.example.gymmanagement.dto.RegisterRequest;
import com.example.gymmanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request
    ) {

        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request
    ) {

        return authService.login(request);
    }
}
