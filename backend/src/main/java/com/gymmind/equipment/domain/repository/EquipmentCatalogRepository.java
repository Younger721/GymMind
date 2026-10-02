package com.gymmind.equipment.domain.repository;

import com.gymmind.equipment.domain.model.EquipmentCatalog;
import java.util.List;
import java.util.Optional;

public interface EquipmentCatalogRepository {
    EquipmentCatalog save(EquipmentCatalog catalog);
    Optional<EquipmentCatalog> findById(Long id);
    List<EquipmentCatalog> findAll();
    List<EquipmentCatalog> findAllByActiveTrue();
}
