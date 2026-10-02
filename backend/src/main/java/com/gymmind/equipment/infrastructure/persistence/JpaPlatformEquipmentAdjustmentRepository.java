package com.gymmind.equipment.infrastructure.persistence;

import com.gymmind.equipment.domain.model.PlatformEquipmentAdjustment;
import com.gymmind.equipment.domain.repository.PlatformEquipmentAdjustmentRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaPlatformEquipmentAdjustmentRepository implements PlatformEquipmentAdjustmentRepository {
    private final SpringDataPlatformEquipmentAdjustmentRepository delegate;

    JpaPlatformEquipmentAdjustmentRepository(SpringDataPlatformEquipmentAdjustmentRepository delegate) {
        this.delegate = delegate;
    }

    public PlatformEquipmentAdjustment save(PlatformEquipmentAdjustment adjustment) { return delegate.save(adjustment); }
}
