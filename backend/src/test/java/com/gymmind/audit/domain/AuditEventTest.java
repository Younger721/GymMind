package com.gymmind.audit.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AuditEventTest {

    @Test
    void rejectsSensitiveMetadataKeys() {
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(
                "USER_UPDATED", "USER", 1L, "SUCCESS", "trace-1", Map.of("password", "secret")));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(
                "USER_UPDATED", "USER", 1L, "SUCCESS", "trace-1", Map.of("healthProfile", "injury")));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(
                "PAYMENT_RECORDED", "PAYMENT", 1L, "SUCCESS", "trace-1", Map.of("cardNumber", "4111")));
    }
}
