package com.gymmind.audit.api;

import com.gymmind.audit.application.AuditQueryService;
import com.gymmind.audit.application.AiUsageQueryService;
import com.gymmind.audit.application.AiUsageFilter;
import com.gymmind.audit.application.AiUsagePage;
import com.gymmind.audit.application.AiUsageSummary;
import com.gymmind.audit.application.AiUsageTrendPoint;
import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.Instant;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/audits")
@SecurityRequirement(name = "bearerAuth")
public class AuditQueryController {
    private final AuditQueryService service;
    private final CurrentActorProvider actors;
    private final AiUsageQueryService aiUsage;

    public AuditQueryController(AuditQueryService service, AiUsageQueryService aiUsage, CurrentActorProvider actors) {
        this.service = service;
        this.aiUsage = aiUsage;
        this.actors = actors;
    }

    @GetMapping
    public ApiResponse<List<OperationAudit>> list() {
        return ApiResponse.success(service.list(actors.requireCurrent()));
    }

    @GetMapping("/ai-usage")
    public ApiResponse<List<AiUsageRecord>> aiUsage() {
        return ApiResponse.success(aiUsage.list(actors.requireCurrent()));
    }

    @GetMapping("/ai-usage/page")
    public ApiResponse<AiUsagePage> aiUsagePage(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(aiUsage.search(actors.requireCurrent(),
                new AiUsageFilter(from, to, tenantId, model, status, page, size)));
    }

    @GetMapping("/ai-usage/summary")
    public ApiResponse<AiUsageSummary> aiUsageSummary(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(aiUsage.summary(actors.requireCurrent(),
                new AiUsageFilter(from, to, tenantId, model, status, 0, 1)));
    }

    @GetMapping("/ai-usage/trend")
    public ApiResponse<List<AiUsageTrendPoint>> aiUsageTrend(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(aiUsage.trend(actors.requireCurrent(),
                new AiUsageFilter(from, to, tenantId, model, status, 0, 1)));
    }
}
