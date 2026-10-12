package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "platform_equipment_inventory", uniqueConstraints = @UniqueConstraint(name = "uk_platform_equipment_catalog", columnNames = "catalog_id"))
public class PlatformEquipmentInventory extends AuditableEntity {
    @Column(name = "catalog_id", nullable = false)
    private Long catalogId;
    @Column(name = "warehouse_quantity", nullable = false)
    private int warehouseQuantity;
    @Column(name = "in_transit_quantity", nullable = false)
    private int inTransitQuantity;
    @Column(name = "outbound_quantity", nullable = false)
    private int outboundQuantity;
    @Column(name = "last_adjusted_at")
    private Instant lastAdjustedAt;

    protected PlatformEquipmentInventory() {}

    private PlatformEquipmentInventory(Long catalogId, int warehouseQuantity) {
        if (catalogId == null || catalogId <= 0 || warehouseQuantity < 0) {
            throw new IllegalArgumentException("Invalid platform equipment inventory");
        }
        this.catalogId = catalogId;
        this.warehouseQuantity = warehouseQuantity;
        this.lastAdjustedAt = Instant.now();
    }

    public static PlatformEquipmentInventory create(Long catalogId, int warehouseQuantity) {
        return new PlatformEquipmentInventory(catalogId, warehouseQuantity);
    }

    public void adjust(int delta) {
        if (warehouseQuantity + delta < 0) {
            throw new IllegalArgumentException("Warehouse quantity cannot be negative");
        }
        warehouseQuantity += delta;
        lastAdjustedAt = Instant.now();
    }

    public void registerDelivery(int quantity) {
        if (quantity <= 0 || quantity > warehouseQuantity) {
            throw new IllegalArgumentException("Insufficient warehouse quantity");
        }
        warehouseQuantity -= quantity;
        inTransitQuantity += quantity;
        outboundQuantity += quantity;
    }

    public void confirmReceipt(int quantity) {
        if (quantity <= 0 || quantity > inTransitQuantity) {
            throw new IllegalArgumentException("Invalid receipt quantity");
        }
        inTransitQuantity -= quantity;
    }

    /** 开发演示：按申请/配送汇总回填平台仓数量，保证 W + 在途 + 门店已入库 = 基准库存 */
    public void reconcileFromRequestTotals(int warehouseQuantity, int inTransitQuantity, int outboundQuantity) {
        if (warehouseQuantity < 0 || inTransitQuantity < 0 || outboundQuantity < 0) {
            throw new IllegalArgumentException("Invalid platform inventory reconciliation");
        }
        this.warehouseQuantity = warehouseQuantity;
        this.inTransitQuantity = inTransitQuantity;
        this.outboundQuantity = outboundQuantity;
        this.lastAdjustedAt = Instant.now();
    }

    public Long getCatalogId() { return catalogId; }
    public int getWarehouseQuantity() { return warehouseQuantity; }
    public int getInTransitQuantity() { return inTransitQuantity; }
    public int getOutboundQuantity() { return outboundQuantity; }
    public Instant getLastAdjustedAt() { return lastAdjustedAt; }
}
