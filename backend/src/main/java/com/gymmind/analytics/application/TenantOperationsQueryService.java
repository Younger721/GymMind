package com.gymmind.analytics.application;

import com.gymmind.ai.harness.plugins.OperationsMetricsContext;
import com.gymmind.ai.harness.plugins.OperationsMetricsQuery;
import com.gymmind.shared.security.CurrentActor;

import java.time.LocalDate;
import java.util.List;

public interface TenantOperationsQueryService {

    void requireAccess(CurrentActor actor);

    OperationsMetricsContext queryMetrics(Long tenantId, OperationsMetricsQuery query);

    TenantOperationsSummaryView summary(Long tenantId, OperationsMetricsQuery query, boolean comparePrevious);

    List<TenantOperationsTrendPoint> trend(Long tenantId, LocalDate from, LocalDate to, String groupBy);
}
