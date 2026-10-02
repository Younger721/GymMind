package com.gymmind.equipment.application;

import java.util.List;

public record EquipmentStatisticsView(long tenantCount, long catalogCount, int deliveredQuantity,
                                      int receivedQuantity, long pendingRequests, long waitingReceive,
                                      List<EquipmentInventorySummaryView> inventory) {
    public record EquipmentInventorySummaryView(Long tenantId, String tenantName, String catalogName, int quantity) {}
}
