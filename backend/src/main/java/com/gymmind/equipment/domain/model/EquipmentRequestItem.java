package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "equipment_request_item")
public class EquipmentRequestItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private EquipmentRequest request;
    @Column(name = "catalog_id", nullable = false)
    private Long catalogId;
    @Column(name = "catalog_name", nullable = false, length = 128)
    private String catalogName;
    @Column(name = "requested_quantity", nullable = false)
    private int requestedQuantity;
    @Column(name = "approved_quantity", nullable = false)
    private int approvedQuantity;
    @Column(name = "delivered_quantity", nullable = false)
    private int deliveredQuantity;
    @Column(name = "received_quantity", nullable = false)
    private int receivedQuantity;

    protected EquipmentRequestItem() {
    }

    EquipmentRequestItem(EquipmentRequest request, Long catalogId, String catalogName, int requestedQuantity) {
        if (catalogId == null || catalogId <= 0 || catalogName == null || catalogName.isBlank() || requestedQuantity <= 0) {
            throw new IllegalArgumentException("Invalid equipment request item");
        }
        this.request = request;
        this.catalogId = catalogId;
        this.catalogName = catalogName.trim();
        this.requestedQuantity = requestedQuantity;
    }

    void approve(int quantity) {
        if (quantity < 0 || quantity > requestedQuantity) {
            throw new IllegalArgumentException("Approved quantity exceeds requested quantity");
        }
        approvedQuantity = quantity;
    }

    void deliver(int quantity) {
        if (quantity < 0 || quantity > approvedQuantity) {
            throw new IllegalArgumentException("Delivered quantity exceeds approved quantity");
        }
        deliveredQuantity = quantity;
    }

    void receive() {
        receivedQuantity = deliveredQuantity;
    }

    public Long getCatalogId() { return catalogId; }
    public String getCatalogName() { return catalogName; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getApprovedQuantity() { return approvedQuantity; }
    public int getDeliveredQuantity() { return deliveredQuantity; }
    public int getReceivedQuantity() { return receivedQuantity; }
}
