package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;
import com.gymmind.shared.security.CurrentActor;

import java.util.List;

public interface AuditQueryService {
    List<OperationAudit> list(CurrentActor actor);
}
