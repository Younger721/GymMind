package com.gymmind.equipment.application;

import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import com.gymmind.shared.security.CurrentActor;
import java.util.List;
import java.util.Map;

public interface EquipmentService {
    List<EquipmentCatalogView> listCatalog(CurrentActor actor, boolean activeOnly);
    EquipmentCatalogView createCatalog(CurrentActor actor, String name, String category, String description, String unit);
    EquipmentCatalogView updateCatalog(CurrentActor actor, Long id, String name, String category, String description, String unit, boolean active);
    List<EquipmentInventoryView> listInventory(CurrentActor actor, Long tenantId);
    List<EquipmentRequestView> listRequests(CurrentActor actor, Long tenantId, EquipmentRequestStatus status);
    EquipmentRequestView createRequest(CurrentActor actor, String note, List<RequestItemCommand> items);
    EquipmentRequestView approve(CurrentActor actor, Long id, Map<Long, Integer> quantities, String comment);
    EquipmentRequestView reject(CurrentActor actor, Long id, String comment);
    EquipmentRequestView deliver(CurrentActor actor, Long id, Map<Long, Integer> quantities, String note);
    EquipmentRequestView receive(CurrentActor actor, Long id);
    List<EquipmentDeliveryView> listDeliveries(CurrentActor actor, Long tenantId);
    EquipmentStatisticsView statistics(CurrentActor actor, Long tenantId);
    List<PlatformEquipmentInventoryView> listWarehouseInventory(CurrentActor actor);
    List<PlatformEquipmentInventoryView> adjustWarehouseInventory(CurrentActor actor, Long catalogId, int delta, String reason);

    record RequestItemCommand(Long catalogId, int quantity) {}
}
