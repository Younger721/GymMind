package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "tenant_equipment", uniqueConstraints = @UniqueConstraint(name = "uk_tenant_equipment_catalog", columnNames = {"tenant_id", "catalog_id"}))
public class EquipmentInventory extends TenantScopedEntity {
    @Column(name = "catalog_id", nullable = false)
    private Long catalogId;
    @Column(name = "delivered_quantity", nullable = false)
    private int deliveredQuantity;
    @Column(name = "received_quantity", nullable = false)
    private int receivedQuantity;
    @Column(name = "usable_quantity", nullable = false)
    private int usableQuantity;
    @Column(name = "abnormal_quantity", nullable = false)
    private int abnormalQuantity;
    @Column(name = "last_received_at")
    private Instant lastReceivedAt;

    protected EquipmentInventory() {}
    private EquipmentInventory(Long tenantId, Long catalogId) { super(tenantId); this.catalogId = catalogId; }
    public static EquipmentInventory create(Long tenantId, Long catalogId) { return new EquipmentInventory(tenantId, catalogId); }
    public void registerDelivery(int quantity) { if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive"); deliveredQuantity += quantity; }
    public void confirmReceipt(int quantity) { if (quantity <= 0 || receivedQuantity + quantity > deliveredQuantity) throw new IllegalArgumentException("Invalid receipt quantity"); receivedQuantity += quantity; usableQuantity += quantity; lastReceivedAt = Instant.now(); }
    public Long getCatalogId() { return catalogId; }
    public int getDeliveredQuantity() { return deliveredQuantity; }
    public int getReceivedQuantity() { return receivedQuantity; }
    public int getUsableQuantity() { return usableQuantity; }
    public int getAbnormalQuantity() { return abnormalQuantity; }
    public Instant getLastReceivedAt() { return lastReceivedAt; }
}
