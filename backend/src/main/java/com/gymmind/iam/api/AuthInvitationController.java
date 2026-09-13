package com.gymmind.iam.api;

import com.gymmind.iam.api.request.AcceptInvitationRequest;
import com.gymmind.iam.api.response.AuthResponse;
import com.gymmind.iam.application.UserInvitationService;
import com.gymmind.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth/invitations")
public class AuthInvitationController {
    private final UserInvitationService service;

    public AuthInvitationController(UserInvitationService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @PostMapping("/accept")
    public ApiResponse<AuthResponse> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        return ApiResponse.success(AuthResponse.from(service.accept(request.toCommand())));
    }
}
