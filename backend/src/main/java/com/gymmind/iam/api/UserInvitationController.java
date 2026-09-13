package com.gymmind.iam.api;

import com.gymmind.iam.api.request.InviteUserRequest;
import com.gymmind.iam.api.response.InvitationResponse;
import com.gymmind.iam.application.UserInvitationService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/users/invitations")
public class UserInvitationController {
    private final UserInvitationService service;
    private final CurrentActorProvider actors;

    public UserInvitationController(UserInvitationService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResponse> invite(@Valid @RequestBody InviteUserRequest request) {
        return ApiResponse.success(InvitationResponse.from(service.invite(actors.requireCurrent(), request.toCommand())));
    }
}
