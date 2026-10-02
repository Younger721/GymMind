package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "equipment_delivery")
public class EquipmentDelivery extends TenantScopedEntity {
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;
    @Column(nullable = false)
    private Instant deliveredAt;
    @Column(length = 1000)
    private String note;

    protected EquipmentDelivery() {}
    private EquipmentDelivery(Long tenantId, Long requestId, Long operatorId, int totalQuantity, String note) {
        super(tenantId);
        this.requestId = requestId;
        this.operatorId = operatorId;
        this.totalQuantity = totalQuantity;
        this.deliveredAt = Instant.now();
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
    public static EquipmentDelivery create(Long tenantId, Long requestId, Long operatorId, int totalQuantity, String note) {
        if (tenantId == null || requestId == null || operatorId == null || totalQuantity <= 0) throw new IllegalArgumentException("Invalid delivery");
        return new EquipmentDelivery(tenantId, requestId, operatorId, totalQuantity, note);
    }
    public Long getRequestId() { return requestId; }
    public Long getOperatorId() { return operatorId; }
    public int getTotalQuantity() { return totalQuantity; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getNote() { return note; }
}
