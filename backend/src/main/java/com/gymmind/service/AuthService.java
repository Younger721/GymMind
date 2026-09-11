package com.gymmind.service;

import com.gymmind.common.exception.BusinessException;
import com.gymmind.dto.auth.AuthResponse;
import com.gymmind.dto.auth.LoginRequest;
import com.gymmind.dto.auth.RegisterRequest;
import com.gymmind.entity.User;
import com.gymmind.repository.UserRepository;
import com.gymmind.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(401, "Invalid username or password"));

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }

        // Check user status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessException(403, "User account is not active");
        }

        log.info("User logged in successfully: {}", user.getUsername());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }
}
