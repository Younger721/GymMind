package com.gymmind.audit.application;

import com.gymmind.audit.domain.AuditEvent;

public interface AuditRecorder {
    void record(AuditEvent event);
}
