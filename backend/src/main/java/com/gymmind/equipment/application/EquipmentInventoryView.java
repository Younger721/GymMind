package com.gymmind.equipment.application;

import java.time.Instant;

public record EquipmentInventoryView(Long tenantId, String tenantName, Long catalogId, String name, String category,
                                     String unit, int deliveredQuantity, int receivedQuantity, int usableQuantity,
                                     int abnormalQuantity, Instant lastReceivedAt) {
}
