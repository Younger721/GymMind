package com.gymmind.audit.infrastructure.persistence;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
class JpaOperationAuditRepository implements OperationAuditRepository {
    private final SpringDataOperationAuditRepository delegate;

    JpaOperationAuditRepository(SpringDataOperationAuditRepository delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public OperationAudit save(OperationAudit audit) {
        return delegate.save(audit);
    }

    @Override
    public List<OperationAudit> findAllByTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        return delegate.findAllByTenantId(tenantId);
    }
}
