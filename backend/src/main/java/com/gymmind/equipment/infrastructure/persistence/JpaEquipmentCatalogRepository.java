package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.EquipmentCatalog;
import com.gymmind.equipment.domain.repository.EquipmentCatalogRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
class JpaEquipmentCatalogRepository implements EquipmentCatalogRepository {
    private final SpringDataEquipmentCatalogRepository delegate;
    JpaEquipmentCatalogRepository(SpringDataEquipmentCatalogRepository delegate) { this.delegate = delegate; }
    public EquipmentCatalog save(EquipmentCatalog catalog) { return delegate.save(catalog); }
    public Optional<EquipmentCatalog> findById(Long id) { return delegate.findById(id); }
    public List<EquipmentCatalog> findAll() { return delegate.findAll(); }
    public List<EquipmentCatalog> findAllByActiveTrue() { return delegate.findAllByActiveTrueOrderByCategoryAscNameAsc(); }
}
