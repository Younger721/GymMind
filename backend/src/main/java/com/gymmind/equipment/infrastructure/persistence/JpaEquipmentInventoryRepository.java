package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentInventory;
import com.gymmind.equipment.domain.repository.EquipmentInventoryRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
class JpaEquipmentInventoryRepository implements EquipmentInventoryRepository {
    private final SpringDataEquipmentInventoryRepository delegate;
    JpaEquipmentInventoryRepository(SpringDataEquipmentInventoryRepository delegate) { this.delegate = delegate; }
    public EquipmentInventory save(EquipmentInventory inventory) { return delegate.save(inventory); }
    public Optional<EquipmentInventory> findByTenantIdAndCatalogId(Long tenantId, Long catalogId) { return delegate.findByTenantIdAndCatalogId(tenantId, catalogId); }
    public List<EquipmentInventory> findAllByTenantId(Long tenantId) { return delegate.findAllByTenantId(tenantId); }
    public List<EquipmentInventory> findAll() { return delegate.findAll(); }

    @Override
    public void deleteAll() {
        delegate.deleteAll();
    }
}
