package com.gymmind.audit.application;

import com.gymmind.audit.domain.AuditEvent;
import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.CurrentActorProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class DefaultAuditRecorder implements AuditRecorder {
    private final OperationAuditRepository repository;
    private final CurrentActorProvider actors;
    private final RejectedAuditWriter rejectedAuditWriter;

    public DefaultAuditRecorder(OperationAuditRepository repository, CurrentActorProvider actors,
                                RejectedAuditWriter rejectedAuditWriter) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.actors = Objects.requireNonNull(actors, "actors");
        this.rejectedAuditWriter = Objects.requireNonNull(rejectedAuditWriter, "rejectedAuditWriter");
    }

    @Override
    @Transactional
    public void record(AuditEvent event) {
        Objects.requireNonNull(event, "event");
        CurrentActor actor = actors.requireCurrent();
        OperationAudit audit = OperationAudit.create(actor.userId(), actor.tenantId(), event.action(),
                event.resourceType(), event.resourceId(), event.auditResult(), event.traceId(), event.metadata());
        if (event.auditResult() == com.gymmind.audit.domain.AuditResult.REJECTED) {
            rejectedAuditWriter.record(audit);
        } else {
            repository.save(audit);
        }
    }
}
