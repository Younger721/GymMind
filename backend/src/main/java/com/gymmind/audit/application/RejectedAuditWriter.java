package com.gymmind.audit.application;

import com.gymmind.audit.domain.OperationAudit;

public interface RejectedAuditWriter {
    void record(OperationAudit audit);
}
