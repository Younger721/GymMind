package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "platform_equipment_adjustment")
public class PlatformEquipmentAdjustment extends AuditableEntity {
    @Column(name = "catalog_id", nullable = false)
    private Long catalogId;
    @Column(nullable = false)
    private int delta;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    @Column(name = "adjusted_at", nullable = false)
    private Instant adjustedAt;

    protected PlatformEquipmentAdjustment() {}

    private PlatformEquipmentAdjustment(Long catalogId, int delta, String reason, Long operatorId) {
        if (catalogId == null || catalogId <= 0 || delta == 0 || reason == null || reason.isBlank() || operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("Invalid platform equipment adjustment");
        }
        this.catalogId = catalogId;
        this.delta = delta;
        this.reason = reason.trim();
        this.operatorId = operatorId;
        this.adjustedAt = Instant.now();
    }

    public static PlatformEquipmentAdjustment create(Long catalogId, int delta, String reason, Long operatorId) {
        return new PlatformEquipmentAdjustment(catalogId, delta, reason, operatorId);
    }

    public Long getCatalogId() { return catalogId; }
    public int getDelta() { return delta; }
    public String getReason() { return reason; }
    public Long getOperatorId() { return operatorId; }
    public Instant getAdjustedAt() { return adjustedAt; }
}
