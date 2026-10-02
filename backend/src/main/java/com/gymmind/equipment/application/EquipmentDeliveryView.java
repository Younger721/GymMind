package com.gymmind.equipment.application;

import java.time.Instant;

public record EquipmentDeliveryView(Long id, Long tenantId, String tenantName, Long requestId, Long operatorId,
                                    int totalQuantity, Instant deliveredAt, String note) {
}
