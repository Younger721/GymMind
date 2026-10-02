package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.PlatformEquipmentInventory;
import com.gymmind.equipment.domain.repository.PlatformEquipmentInventoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaPlatformEquipmentInventoryRepository implements PlatformEquipmentInventoryRepository {
    private final SpringDataPlatformEquipmentInventoryRepository delegate;

    JpaPlatformEquipmentInventoryRepository(SpringDataPlatformEquipmentInventoryRepository delegate) {
        this.delegate = delegate;
    }

    public PlatformEquipmentInventory save(PlatformEquipmentInventory inventory) { return delegate.save(inventory); }
    public Optional<PlatformEquipmentInventory> findByCatalogId(Long catalogId) { return delegate.findByCatalogId(catalogId); }
    public List<PlatformEquipmentInventory> findAll() { return delegate.findAllByOrderByCatalogIdAsc(); }
}
