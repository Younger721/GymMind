package com.gymmind.audit.infrastructure.persistence;

import com.gymmind.audit.domain.OperationAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataOperationAuditRepository extends JpaRepository<OperationAudit, Long> {
    List<OperationAudit> findAllByTenantId(Long tenantId);
}
