package com.gymmind.equipment.application;

import java.util.List;

public record EquipmentStatisticsView(long tenantCount, long catalogCount, int deliveredQuantity,
                                      int receivedQuantity, long pendingRequests, long waitingReceive,
                                      List<EquipmentInventorySummaryView> inventory,
                                      int warehouseQuantity, int inTransitQuantity, int platformTotalQuantity,
                                      List<PlatformEquipmentInventoryView> warehouseInventory) {
    public record EquipmentInventorySummaryView(Long tenantId, String tenantName, String catalogName, int quantity) {}
}
