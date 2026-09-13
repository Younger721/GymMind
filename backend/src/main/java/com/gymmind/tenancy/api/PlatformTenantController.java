package com.gymmind.tenancy.api;

import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.tenancy.api.request.CreateTenantRequest;
import com.gymmind.tenancy.api.response.TenantView;
import com.gymmind.tenancy.application.PlatformTenantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@SecurityRequirement(name = "bearerAuth")
public class PlatformTenantController {

    private final PlatformTenantService service;
    private final CurrentActorProvider actors;

    public PlatformTenantController(PlatformTenantService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantView> create(@Valid @RequestBody CreateTenantRequest request) {
        CurrentActor actor = requirePlatform("platform:tenant:write");
        return ApiResponse.success(service.create(request.toCommand(), actor));
    }

    @GetMapping
    public ApiResponse<PageResponse<TenantView>> list(Pageable pageable) {
        return ApiResponse.success(service.list(requirePlatform("platform:tenant:read"), pageable));
    }

    @PatchMapping("/{tenantId}/activate")
    public ApiResponse<TenantView> activate(@PathVariable Long tenantId) {
        return ApiResponse.success(service.activate(tenantId, requirePlatform("platform:tenant:write")));
    }

    @PatchMapping("/{tenantId}/disable")
    public ApiResponse<TenantView> disable(@PathVariable Long tenantId) {
        return ApiResponse.success(service.disable(tenantId, requirePlatform("platform:tenant:write")));
    }

    private CurrentActor requirePlatform(String permission) {
        CurrentActor actor = actors.requireCurrent();
        if (!actor.isPlatformAdmin() || !actor.hasPermission(permission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return actor;
    }
}
