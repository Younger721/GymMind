package com.gymmind.iam.api;

import com.gymmind.iam.api.request.ChangeUserStatusRequest;
import com.gymmind.iam.api.request.CreateUserRequest;
import com.gymmind.iam.api.request.ReplaceUserRolesRequest;
import com.gymmind.iam.api.response.UserResponse;
import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAdministrationService service;
    private final CurrentActorProvider actors;

    public UserController(UserAdministrationService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        CurrentActor actor = requireTenantAdmin();
        return ApiResponse.success(UserResponse.from(service.create(actor, request.toCommand())));
    }

    @GetMapping
    public ApiResponse<PageResponse<UserAdministrationService.UserSummary>> list(Pageable pageable) {
        return ApiResponse.success(service.list(requireTenantAdmin(), pageable));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long userId,
                                          @Valid @RequestBody ChangeUserStatusRequest request) {
        CurrentActor actor = requireTenantAdmin();
        service.changeStatus(actor, userId, request.status());
        return ApiResponse.success(null);
    }

    @PutMapping("/{userId}/roles")
    public ApiResponse<Void> replaceRoles(@PathVariable Long userId,
                                          @Valid @RequestBody ReplaceUserRolesRequest request) {
        CurrentActor actor = requireTenantAdmin();
        service.replaceRoles(actor, userId, request.roles());
        return ApiResponse.success(null);
    }

    private CurrentActor requireTenantAdmin() {
        CurrentActor actor = actors.requireCurrent();
        if (actor.tenantId() == null || !actor.roles().contains(com.gymmind.iam.domain.model.RoleCode.GYM_ADMIN)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return actor;
    }
}
