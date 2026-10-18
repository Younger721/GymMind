package com.gymmind.iam.api;

import com.gymmind.iam.application.PlatformTenantAdminService;
import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.api.PageResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/platform/tenants/{tenantId}/administrators")
public class PlatformTenantAdminController {
    private final PlatformTenantAdminService service;
    private final CurrentActorProvider actors;

    public PlatformTenantAdminController(PlatformTenantAdminService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service);
        this.actors = Objects.requireNonNull(actors);
    }

    @GetMapping
    public ApiResponse<PageResponse<PlatformTenantAdminService.AdministratorView>> list(
            @PathVariable Long tenantId, Pageable pageable) {
        return ApiResponse.success(service.list(actors.requireCurrent(), tenantId, pageable));
    }

    @PutMapping("/{userId}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long tenantId, @PathVariable Long userId,
                                           @Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(actors.requireCurrent(), tenantId, userId, request.password());
        return ApiResponse.success(null);
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long tenantId, @PathVariable Long userId,
                                          @Valid @RequestBody ChangeStatusRequest request) {
        service.changeStatus(actors.requireCurrent(), tenantId, userId, request.status());
        return ApiResponse.success(null);
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 6, max = 72) String password) {}
    public record ChangeStatusRequest(UserStatus status) {}
}
