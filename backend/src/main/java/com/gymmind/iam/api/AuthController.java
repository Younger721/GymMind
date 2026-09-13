package com.gymmind.iam.api;

import com.gymmind.iam.api.request.LoginRequest;
import com.gymmind.iam.api.request.LogoutRequest;
import com.gymmind.iam.api.request.RefreshRequest;
import com.gymmind.iam.api.request.RegisterRequest;
import com.gymmind.iam.api.response.AuthResponse;
import com.gymmind.iam.application.AuthApplicationService;
import com.gymmind.iam.application.command.LoginCommand;
import com.gymmind.iam.application.command.LogoutCommand;
import com.gymmind.iam.application.command.RefreshCommand;
import com.gymmind.iam.application.command.RegisterTenantCommand;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthApplicationService authService;
    private final CurrentActorProvider actors;

    public AuthController(AuthApplicationService authService, CurrentActorProvider actors) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(AuthResponse.from(authService.registerTenant(new RegisterTenantCommand(
                request.tenantCode(), request.tenantName(), request.email(),
                request.password(), request.displayName()))));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(AuthResponse.from(
                authService.login(new LoginCommand(request.email(), request.password()))));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(AuthResponse.from(
                authService.refresh(new RefreshCommand(request.refreshToken()))));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody LogoutRequest request) {
        authService.logout(
                actors.requireCurrent(),
                new LogoutCommand(bearerToken(authorization), request.refreshToken()));
        return ApiResponse.success(null);
    }

    private static String bearerToken(String authorization) {
        if (authorization == null
                || authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
        return token;
    }
}
