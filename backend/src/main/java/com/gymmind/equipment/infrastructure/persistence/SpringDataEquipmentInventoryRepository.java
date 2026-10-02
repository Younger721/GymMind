package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface SpringDataEquipmentInventoryRepository extends JpaRepository<EquipmentInventory, Long> {
    Optional<EquipmentInventory> findByTenantIdAndCatalogId(Long tenantId, Long catalogId);
    List<EquipmentInventory> findAllByTenantId(Long tenantId);
}
