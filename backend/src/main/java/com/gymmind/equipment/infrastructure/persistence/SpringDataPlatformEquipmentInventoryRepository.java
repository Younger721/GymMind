package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.PlatformEquipmentInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataPlatformEquipmentInventoryRepository extends JpaRepository<PlatformEquipmentInventory, Long> {
    Optional<PlatformEquipmentInventory> findByCatalogId(Long catalogId);
    List<PlatformEquipmentInventory> findAllByOrderByCatalogIdAsc();
}
