package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.auth.AuthResponse;
import com.gymmind.dto.auth.LoginRequest;
import com.gymmind.dto.auth.RegisterRequest;
import com.gymmind.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success("Registration successful", response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("Login successful", response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // JWT is stateless, logout is handled on client side by removing token
        return ApiResponse.success("Logout successful", null);
    }
}
