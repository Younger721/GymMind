package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.PlatformEquipmentAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPlatformEquipmentAdjustmentRepository extends JpaRepository<PlatformEquipmentAdjustment, Long> {
}
