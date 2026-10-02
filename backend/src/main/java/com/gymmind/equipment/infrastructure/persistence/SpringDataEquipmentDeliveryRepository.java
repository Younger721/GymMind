package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface SpringDataEquipmentDeliveryRepository extends JpaRepository<EquipmentDelivery, Long> {
    List<EquipmentDelivery> findAllByTenantIdOrderByIdDesc(Long tenantId);
}
