package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface SpringDataEquipmentCatalogRepository extends JpaRepository<EquipmentCatalog, Long> {
    List<EquipmentCatalog> findAllByActiveTrueOrderByCategoryAscNameAsc();
}
