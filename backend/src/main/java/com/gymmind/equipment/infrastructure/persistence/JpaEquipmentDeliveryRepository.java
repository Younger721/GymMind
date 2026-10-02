package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentDelivery;
import com.gymmind.equipment.domain.repository.EquipmentDeliveryRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
class JpaEquipmentDeliveryRepository implements EquipmentDeliveryRepository {
    private final SpringDataEquipmentDeliveryRepository delegate;
    JpaEquipmentDeliveryRepository(SpringDataEquipmentDeliveryRepository delegate) { this.delegate = delegate; }
    public EquipmentDelivery save(EquipmentDelivery delivery) { return delegate.save(delivery); }
    public List<EquipmentDelivery> findAllByTenantId(Long tenantId) { return delegate.findAllByTenantIdOrderByIdDesc(tenantId); }
    public List<EquipmentDelivery> findAll() { return delegate.findAll(); }
}
