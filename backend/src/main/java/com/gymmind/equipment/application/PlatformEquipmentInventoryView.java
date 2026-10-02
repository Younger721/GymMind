package com.gymmind.equipment.application;

import java.time.Instant;

public record PlatformEquipmentInventoryView(Long catalogId, String name, String category, String unit,
                                             int warehouseQuantity, int inTransitQuantity, int outboundQuantity,
                                             int tenantReceivedQuantity, int totalQuantity, Instant lastAdjustedAt) {
}
