package com.gymmind.audit.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Caller-supplied audit facts. Actor and tenant are deliberately absent. */
public record AuditEvent(
        String action,
        String resourceType,
        Long resourceId,
        String result,
        String traceId,
        Map<String, String> metadata) {

    public AuditEvent(String action, String resourceType, Long resourceId, AuditResult result,
                      String traceId, Map<String, String> metadata) {
        this(action, resourceType, resourceId, result == null ? null : result.name(), traceId, metadata);
    }

    private static final Set<String> ALLOWED_METADATA = Set.of(
            "reason", "source", "endpoint", "httpStatus", "requestId", "changedFields", "resourceName");
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(password|passwd|token|authorization|secret|health|medical|diagnos|bank|card|cvv|cvc|accountnumber)");
    private static final Pattern CARD_NUMBER = Pattern.compile("(?:\\d[ -]?){13,19}");

    public AuditEvent {
        action = require(action, "action");
        resourceType = require(resourceType, "resourceType");
        result = require(result, "result").toUpperCase(Locale.ROOT);
        try {
            AuditResult.valueOf(result);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported audit result", ex);
        }
        traceId = require(traceId, "traceId");
        if (resourceId != null && resourceId <= 0) {
            throw new IllegalArgumentException("resourceId must be positive");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        metadata = Map.copyOf(metadata);
        metadata.forEach((key, value) -> {
            if (key == null || !ALLOWED_METADATA.contains(key)) {
                throw new IllegalArgumentException("Metadata key is not allowed: " + key);
            }
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Metadata value must not be blank");
            }
            if (SENSITIVE.matcher(key).find() || SENSITIVE.matcher(value).find()
                    || CARD_NUMBER.matcher(value).find()) {
                throw new IllegalArgumentException("Sensitive metadata is not allowed");
            }
        });
    }

    public AuditResult auditResult() {
        return AuditResult.valueOf(result);
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
