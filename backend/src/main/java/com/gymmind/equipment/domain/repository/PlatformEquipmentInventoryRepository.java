package com.gymmind.equipment.domain.repository;

import com.gymmind.equipment.domain.model.PlatformEquipmentInventory;

import java.util.List;
import java.util.Optional;

public interface PlatformEquipmentInventoryRepository {
    PlatformEquipmentInventory save(PlatformEquipmentInventory inventory);
    Optional<PlatformEquipmentInventory> findByCatalogId(Long catalogId);
    List<PlatformEquipmentInventory> findAll();
}
