package com.gymmind.audit.domain;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.util.Map;

@Entity
@Table(name = "sys_operation_audit")
public class OperationAudit extends AuditableEntity {

    @Column(name = "actor_id", nullable = false, updatable = false)
    private Long actorId;

    @Column(name = "tenant_id", updatable = false)
    private Long tenantId;

    @Column(nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, updatable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 16)
    private AuditResult result;

    @Column(name = "trace_id", nullable = false, updatable = false, length = 128)
    private String traceId;

    @Column(name = "metadata_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String metadataJson;

    protected OperationAudit() {
    }

    private OperationAudit(Long actorId, Long tenantId, String action, String resourceType, Long resourceId,
                           AuditResult result, String traceId, String metadataJson) {
        if (actorId == null || actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
        this.actorId = actorId;
        this.tenantId = tenantId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.result = result;
        this.traceId = traceId;
        this.metadataJson = metadataJson;
    }

    public static OperationAudit create(Long actorId, Long tenantId, String action, String resourceType,
                                        Long resourceId, AuditResult result, String traceId,
                                        Map<String, String> metadata) {
        AuditEvent event = new AuditEvent(action, resourceType, resourceId, result.name(), traceId, metadata);
        return new OperationAudit(actorId, tenantId, event.action(), event.resourceType(), event.resourceId(),
                event.auditResult(), event.traceId(), toJson(event.metadata()));
    }

    @PreUpdate
    public void assertNotUpdatable() {
        throw new UnsupportedOperationException("Operation audits are append-only");
    }

    @PreRemove
    public void assertNotRemovable() {
        throw new UnsupportedOperationException("Operation audits cannot be removed");
    }

    public Long getActorId() { return actorId; }
    public Long getTenantId() { return tenantId; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public AuditResult getResult() { return result; }
    public String getTraceId() { return traceId; }
    public String getMetadataJson() { return metadataJson; }

    private static String toJson(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
