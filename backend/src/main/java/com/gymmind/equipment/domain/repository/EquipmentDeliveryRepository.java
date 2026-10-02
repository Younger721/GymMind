package com.gymmind.equipment.domain.repository;

import com.gymmind.equipment.domain.model.EquipmentDelivery;
import java.util.List;

public interface EquipmentDeliveryRepository {
    EquipmentDelivery save(EquipmentDelivery delivery);
    List<EquipmentDelivery> findAllByTenantId(Long tenantId);
    List<EquipmentDelivery> findAll();
}
