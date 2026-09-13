package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
class DefaultRejectedAuditWriter implements RejectedAuditWriter {
    private final OperationAuditRepository repository;

    DefaultRejectedAuditWriter(OperationAuditRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationAudit audit) {
        repository.save(audit);
    }
}
