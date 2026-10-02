package com.gymmind.equipment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformEquipmentInventoryTest {

    @Test
    void deliveryMovesQuantityFromWarehouseToTransitAndOutbound() {
        PlatformEquipmentInventory inventory = PlatformEquipmentInventory.create(3L, 10);

        inventory.registerDelivery(4);

        assertThat(inventory.getWarehouseQuantity()).isEqualTo(6);
        assertThat(inventory.getInTransitQuantity()).isEqualTo(4);
        assertThat(inventory.getOutboundQuantity()).isEqualTo(4);
    }

    @Test
    void receiptRemovesQuantityFromTransit() {
        PlatformEquipmentInventory inventory = PlatformEquipmentInventory.create(3L, 10);
        inventory.registerDelivery(4);

        inventory.confirmReceipt(4);

        assertThat(inventory.getWarehouseQuantity()).isEqualTo(6);
        assertThat(inventory.getInTransitQuantity()).isZero();
        assertThat(inventory.getOutboundQuantity()).isEqualTo(4);
    }

    @Test
    void rejectsDeliveryWhenWarehouseQuantityIsInsufficient() {
        PlatformEquipmentInventory inventory = PlatformEquipmentInventory.create(3L, 2);

        assertThatThrownBy(() -> inventory.registerDelivery(3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adjustsWarehouseQuantityWithoutAllowingNegativeStock() {
        PlatformEquipmentInventory inventory = PlatformEquipmentInventory.create(3L, 10);

        inventory.adjust(-4);

        assertThat(inventory.getWarehouseQuantity()).isEqualTo(6);
        assertThatThrownBy(() -> inventory.adjust(-7))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
