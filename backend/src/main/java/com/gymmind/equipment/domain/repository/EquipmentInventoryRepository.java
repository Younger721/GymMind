package com.gymmind.equipment.domain.repository;

import com.gymmind.equipment.domain.model.EquipmentInventory;
import java.util.List;
import java.util.Optional;

public interface EquipmentInventoryRepository {
    EquipmentInventory save(EquipmentInventory inventory);
    Optional<EquipmentInventory> findByTenantIdAndCatalogId(Long tenantId, Long catalogId);
    List<EquipmentInventory> findAllByTenantId(Long tenantId);
    List<EquipmentInventory> findAll();

    /** 开发演示：清空门店库存后按申请单重建 */
    void deleteAll();
}
