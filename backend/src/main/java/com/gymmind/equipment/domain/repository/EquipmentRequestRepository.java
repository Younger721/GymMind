package com.gymmind.equipment.domain.repository;

import com.gymmind.equipment.domain.model.EquipmentRequest;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import java.util.List;
import java.util.Optional;

public interface EquipmentRequestRepository {
    EquipmentRequest save(EquipmentRequest request);
    Optional<EquipmentRequest> findByTenantIdAndId(Long tenantId, Long id);
    Optional<EquipmentRequest> findById(Long id);
    List<EquipmentRequest> findAllByTenantId(Long tenantId);
    List<EquipmentRequest> findAll();
    List<EquipmentRequest> findAllByStatus(EquipmentRequestStatus status);
}
