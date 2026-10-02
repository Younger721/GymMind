package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentRequest;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import com.gymmind.equipment.domain.repository.EquipmentRequestRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
class JpaEquipmentRequestRepository implements EquipmentRequestRepository {
    private final SpringDataEquipmentRequestRepository delegate;
    JpaEquipmentRequestRepository(SpringDataEquipmentRequestRepository delegate) { this.delegate = delegate; }
    public EquipmentRequest save(EquipmentRequest request) { return delegate.save(request); }
    public Optional<EquipmentRequest> findByTenantIdAndId(Long tenantId, Long id) { return delegate.findByTenantIdAndId(tenantId, id); }
    public Optional<EquipmentRequest> findById(Long id) { return delegate.findById(id); }
    public List<EquipmentRequest> findAllByTenantId(Long tenantId) { return delegate.findAllByTenantIdOrderByIdDesc(tenantId); }
    public List<EquipmentRequest> findAll() { return delegate.findAll(); }
    public List<EquipmentRequest> findAllByStatus(EquipmentRequestStatus status) { return delegate.findAllByStatusOrderByIdDesc(status); }
}
