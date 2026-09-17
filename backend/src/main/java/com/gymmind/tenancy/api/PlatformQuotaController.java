package com.gymmind.tenancy.api;

import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.tenancy.application.TenantQuotaService;
import com.gymmind.tenancy.application.TenantQuotaView;
import com.gymmind.tenancy.application.UpdateTenantQuotaCommand;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/tenants")
@SecurityRequirement(name = "bearerAuth")
public class PlatformQuotaController {

    private final TenantQuotaService service;
    private final CurrentActorProvider actors;

    public PlatformQuotaController(TenantQuotaService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping("/{tenantId}/quota")
    public ApiResponse<TenantQuotaView> get(@PathVariable Long tenantId) {
        return ApiResponse.success(service.getForPlatform(actors.requireCurrent(), tenantId));
    }

    @PutMapping("/{tenantId}/quota")
    public ApiResponse<TenantQuotaView> update(@PathVariable Long tenantId,
                                               @Valid @RequestBody QuotaRequest request) {
        return ApiResponse.success(service.updateForPlatform(
                actors.requireCurrent(), tenantId, request.toCommand()));
    }

    public record QuotaRequest(
            @Min(1) int maxAgents,
            @Min(1) int maxAiCallsPerMonth,
            @Min(1) int maxKnowledgeDocuments,
            boolean aiModuleEnabled,
            boolean knowledgeModuleEnabled) {
        UpdateTenantQuotaCommand toCommand() {
            return new UpdateTenantQuotaCommand(
                    maxAgents, maxAiCallsPerMonth, maxKnowledgeDocuments,
                    aiModuleEnabled, knowledgeModuleEnabled);
        }
    }
}
