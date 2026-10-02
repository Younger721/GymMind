package com.gymmind.equipment.application;

public record EquipmentRequestItemView(Long catalogId, String name, int requestedQuantity, int approvedQuantity,
                                       int deliveredQuantity, int receivedQuantity) {
}
