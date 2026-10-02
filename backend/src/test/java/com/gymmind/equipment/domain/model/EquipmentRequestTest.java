package com.gymmind.equipment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquipmentRequestTest {

    @Test
    void approvesAndReceivesRequestedQuantity() {
        EquipmentRequest request = EquipmentRequest.create(11L, 7L, "请补充有氧器械");
        EquipmentRequestItem item = request.addItem(3L, "跑步机", 4);

        request.approve(99L, "同意补充", java.util.Map.of(item.getCatalogId(), 3));
        request.deliver(java.util.Map.of(item.getCatalogId(), 3));
        request.receive();

        assertThat(request.getStatus()).isEqualTo(EquipmentRequestStatus.RECEIVED);
        assertThat(item.getApprovedQuantity()).isEqualTo(3);
        assertThat(item.getDeliveredQuantity()).isEqualTo(3);
        assertThat(item.getReceivedQuantity()).isEqualTo(3);
    }

    @Test
    void rejectsApprovalAboveRequestedQuantity() {
        EquipmentRequest request = EquipmentRequest.create(11L, 7L, "需要哑铃");
        EquipmentRequestItem item = request.addItem(3L, "哑铃", 4);

        assertThatThrownBy(() -> request.approve(99L, null, java.util.Map.of(item.getCatalogId(), 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDeliveryAboveApprovedQuantity() {
        EquipmentRequest request = EquipmentRequest.create(11L, 7L, "需要深蹲架");
        EquipmentRequestItem item = request.addItem(3L, "深蹲架", 2);
        request.approve(99L, null, java.util.Map.of(item.getCatalogId(), 1));

        assertThatThrownBy(() -> request.deliver(java.util.Map.of(item.getCatalogId(), 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
