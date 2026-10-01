package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.audit.domain.repository.OperationAuditRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.iam.domain.model.RoleCode;
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
        requireAuditReader(actor);
        Long tenantId = ActorAccess.tenantId(actor);
        return tenantId == null ? repository.findAll() : repository.findAllByTenantId(tenantId);
    }

    private static void requireAuditReader(CurrentActor actor) {
        if (actor == null || (!actor.isPlatformAdmin() && !actor.roles().contains(RoleCode.GYM_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
