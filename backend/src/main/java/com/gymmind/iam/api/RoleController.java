package com.gymmind.iam.api;

import com.gymmind.iam.api.response.RoleResponse;
import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.Set;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/roles")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {
    private final UserAdministrationService service;
    private final CurrentActorProvider actors;

    public RoleController(UserAdministrationService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> list() {
        CurrentActor actor = actors.requireCurrent();
        if (ActorAccess.isSuperAdmin(actor)) {
            ActorAccess.requirePlatformAdmin(actor, "role:read");
        } else {
            ActorAccess.requireGymAdmin(actor, "role:read");
        }
        return ApiResponse.success(service.listRoles(actor).stream().map(RoleResponse::from).toList());
    }

    public record PermissionCatalog(String code, String name, Set<RoleCode> roles) {}
    public record PermissionUpdate(Set<String> permissions) {}

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionCatalog>> permissions() {
        CurrentActor actor = actors.requireCurrent();
        return ApiResponse.success(service.listPermissionCatalog(actor).stream()
                .map(p -> new PermissionCatalog(p.code(), p.name(), p.roles())).toList());
    }

    @PutMapping("/{roleCode}/permissions")
    public ApiResponse<Void> updatePermissions(@PathVariable RoleCode roleCode, @RequestBody PermissionUpdate request) {
        CurrentActor actor = actors.requireCurrent();
        service.replaceRolePermissions(actor, roleCode, request == null ? Set.of() : request.permissions());
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{roleCode}/disable")
    public ApiResponse<Void> disable(@PathVariable RoleCode roleCode) {
        CurrentActor actor = actors.requireCurrent();
        ActorAccess.requirePlatformAdmin(actor, "role:write");
        service.changeRoleStatus(actor, roleCode, false);
        return ApiResponse.success(null);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{roleCode}/enable")
    public ApiResponse<Void> enable(@PathVariable RoleCode roleCode) {
        CurrentActor actor = actors.requireCurrent();
        ActorAccess.requirePlatformAdmin(actor, "role:write");
        service.changeRoleStatus(actor, roleCode, true);
        return ApiResponse.success(null);
    }
}
