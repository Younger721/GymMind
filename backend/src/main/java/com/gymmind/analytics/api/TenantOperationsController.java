package com.gymmind.analytics.api;

import com.gymmind.ai.harness.plugins.OperationsMetricsQuery;
import com.gymmind.analytics.application.TenantOperationsQueryService;
import com.gymmind.analytics.application.TenantOperationsSummaryView;
import com.gymmind.analytics.application.TenantOperationsTrendPoint;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/tenant-operations")
@SecurityRequirement(name = "bearerAuth")
public class TenantOperationsController {

    private final TenantOperationsQueryService tenantOperations;
    private final CurrentActorProvider actors;

    public TenantOperationsController(TenantOperationsQueryService tenantOperations, CurrentActorProvider actors) {
        this.tenantOperations = tenantOperations;
        this.actors = actors;
    }

    @GetMapping("/summary")
    public ApiResponse<TenantOperationsSummaryView> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean comparePrevious) {
        CurrentActor actor = actors.requireCurrent();
        tenantOperations.requireAccess(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        try {
            OperationsMetricsQuery query = new OperationsMetricsQuery(from, to, buildPeriodLabel(from, to));
            return ApiResponse.success(tenantOperations.summary(tenantId, query, comparePrevious));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, exception.getMessage());
        }
    }

    @GetMapping("/trend")
    public ApiResponse<List<TenantOperationsTrendPoint>> trend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String groupBy) {
        CurrentActor actor = actors.requireCurrent();
        tenantOperations.requireAccess(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        try {
            return ApiResponse.success(tenantOperations.trend(tenantId, from, to, groupBy));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, exception.getMessage());
        }
    }

    private static String buildPeriodLabel(LocalDate from, LocalDate to) {
        if (from.getDayOfMonth() == 1 && to.equals(from.plusMonths(1).minusDays(1))) {
            return from.getYear() + "年" + from.getMonthValue() + "月";
        }
        return from + "至" + to;
    }
}
