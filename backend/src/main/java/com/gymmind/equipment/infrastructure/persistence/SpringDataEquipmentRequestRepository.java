package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentRequest;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface SpringDataEquipmentRequestRepository extends JpaRepository<EquipmentRequest, Long> {
    Optional<EquipmentRequest> findByTenantIdAndId(Long tenantId, Long id);
    List<EquipmentRequest> findAllByTenantIdOrderByIdDesc(Long tenantId);
    List<EquipmentRequest> findAllByStatusOrderByIdDesc(EquipmentRequestStatus status);
}
