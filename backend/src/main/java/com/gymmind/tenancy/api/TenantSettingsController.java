package com.gymmind.tenancy.api;

import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.tenancy.api.request.UpdateTenantSettingsRequest;
import com.gymmind.tenancy.api.response.TenantSettingsView;
import com.gymmind.tenancy.application.TenantSettingsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/tenant/settings")
@SecurityRequirement(name = "bearerAuth")
public class TenantSettingsController {

    private final TenantSettingsService service;
    private final CurrentActorProvider actors;

    public TenantSettingsController(TenantSettingsService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @GetMapping
    public ApiResponse<TenantSettingsView> getCurrent() {
        return ApiResponse.success(service.getCurrent(requireGymAdmin("tenant:settings:read")));
    }

    @PutMapping
    public ApiResponse<TenantSettingsView> update(@Valid @RequestBody UpdateTenantSettingsRequest request) {
        return ApiResponse.success(service.updateCurrent(requireGymAdmin("tenant:settings:write"), request.toCommand()));
    }

    private CurrentActor requireGymAdmin(String permission) {
        CurrentActor actor = actors.requireCurrent();
        if (actor.tenantId() == null || !actor.roles().contains(RoleCode.GYM_ADMIN)
                || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return actor;
    }
}
