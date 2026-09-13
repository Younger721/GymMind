package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class DefaultAuditQueryService implements AuditQueryService {
    private final OperationAuditRepository repository;

    public DefaultAuditQueryService(OperationAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperationAudit> list(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null) {
            throw new IllegalArgumentException("Tenant actor is required");
        }
        return repository.findAllByTenantId(actor.tenantId());
    }
}
