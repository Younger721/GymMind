package com.gymmind.audit.domain.repository;

import com.gymmind.audit.domain.OperationAudit;

import java.util.List;

public interface OperationAuditRepository {
    OperationAudit save(OperationAudit audit);
    List<OperationAudit> findAllByTenantId(Long tenantId);
}
