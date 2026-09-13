package com.gymmind.audit.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationAuditTest {

    @Test
    void auditRecordCannotBeMutatedOrDeleted() {
        OperationAudit audit = OperationAudit.create(7L, 11L, "USER_UPDATED", "USER", 1L,
                AuditResult.SUCCESS, "trace-1", Map.of());
        assertThrows(UnsupportedOperationException.class, audit::assertNotUpdatable);
        assertThrows(UnsupportedOperationException.class, audit::assertNotRemovable);
    }
}
