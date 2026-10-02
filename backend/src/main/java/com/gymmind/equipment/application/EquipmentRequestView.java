package com.gymmind.equipment.application;

import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import java.time.Instant;
import java.util.List;

public record EquipmentRequestView(Long id, Long tenantId, String tenantName, Long requesterId,
                                   EquipmentRequestStatus status, String note, Long reviewerId,
                                   Instant reviewedAt, String reviewComment, List<EquipmentRequestItemView> items,
                                   Instant createdAt) {
}
