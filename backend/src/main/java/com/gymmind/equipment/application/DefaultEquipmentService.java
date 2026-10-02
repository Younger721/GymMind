package com.gymmind.equipment.application;

import com.gymmind.equipment.domain.model.EquipmentCatalog;
import com.gymmind.equipment.domain.model.EquipmentDelivery;
import com.gymmind.equipment.domain.model.EquipmentInventory;
import com.gymmind.equipment.domain.model.EquipmentRequest;
import com.gymmind.equipment.domain.model.EquipmentRequestItem;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import com.gymmind.equipment.domain.repository.EquipmentCatalogRepository;
import com.gymmind.equipment.domain.repository.EquipmentDeliveryRepository;
import com.gymmind.equipment.domain.repository.EquipmentInventoryRepository;
import com.gymmind.equipment.domain.repository.EquipmentRequestRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultEquipmentService implements EquipmentService {
    private final EquipmentCatalogRepository catalogs;
    private final EquipmentInventoryRepository inventories;
    private final EquipmentRequestRepository requests;
    private final EquipmentDeliveryRepository deliveries;
    private final TenantRepository tenants;

    public DefaultEquipmentService(EquipmentCatalogRepository catalogs, EquipmentInventoryRepository inventories,
                                   EquipmentRequestRepository requests, EquipmentDeliveryRepository deliveries,
                                   TenantRepository tenants) {
        this.catalogs = catalogs;
        this.inventories = inventories;
        this.requests = requests;
        this.deliveries = deliveries;
        this.tenants = tenants;
    }

    @Transactional(readOnly = true)
    public List<EquipmentCatalogView> listCatalog(CurrentActor actor, boolean activeOnly) {
        requireEquipmentReader(actor);
        List<EquipmentCatalog> source = activeOnly ? catalogs.findAllByActiveTrue() : catalogs.findAll();
        return source.stream().map(EquipmentCatalogView::from).toList();
    }

    @Transactional
    public EquipmentCatalogView createCatalog(CurrentActor actor, String name, String category, String description, String unit) {
        requirePlatform(actor);
        try {
            return EquipmentCatalogView.from(catalogs.save(EquipmentCatalog.create(name, category, description, unit)));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Transactional
    public EquipmentCatalogView updateCatalog(CurrentActor actor, Long id, String name, String category, String description, String unit, boolean active) {
        requirePlatform(actor);
        EquipmentCatalog catalog = catalogs.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        try {
            catalog.update(name, category, description, unit);
            catalog.setActive(active);
            return EquipmentCatalogView.from(catalogs.save(catalog));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<EquipmentInventoryView> listInventory(CurrentActor actor, Long tenantId) {
        requireEquipmentReader(actor);
        Long scope = resolveTenantScope(actor, tenantId);
        List<EquipmentInventory> source = scope == null ? inventories.findAll() : inventories.findAllByTenantId(scope);
        return source.stream().map(this::inventoryView).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipmentRequestView> listRequests(CurrentActor actor, Long tenantId, EquipmentRequestStatus status) {
        requireEquipmentReader(actor);
        List<EquipmentRequest> source;
        if (ActorAccess.isSuperAdmin(actor)) {
            if (tenantId != null) source = requests.findAllByTenantId(tenantId);
            else if (status != null) source = requests.findAllByStatus(status);
            else source = requests.findAll();
            if (tenantId != null && status != null) source = source.stream().filter(item -> item.getStatus() == status).toList();
        } else {
            Long scope = resolveTenantScope(actor, tenantId);
            source = requests.findAllByTenantId(scope);
            if (status != null) source = source.stream().filter(item -> item.getStatus() == status).toList();
        }
        return source.stream().map(this::requestView).toList();
    }

    @Transactional
    public EquipmentRequestView createRequest(CurrentActor actor, String note, List<RequestItemCommand> itemCommands) {
        requireGymAdmin(actor);
        if (itemCommands == null || itemCommands.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        Long tenantId = ActorAccess.requireTenantId(actor);
        EquipmentRequest request = EquipmentRequest.create(tenantId, actor.userId(), note);
        Set<Long> seen = new java.util.HashSet<>();
        try {
            for (RequestItemCommand command : itemCommands) {
                if (command == null || command.catalogId() == null || command.quantity() <= 0 || !seen.add(command.catalogId())) {
                    throw new IllegalArgumentException("Invalid request item");
                }
                EquipmentCatalog catalog = catalogs.findById(command.catalogId()).filter(EquipmentCatalog::isActive)
                        .orElseThrow(() -> new IllegalArgumentException("Catalog is not active"));
                request.addItem(catalog.getId(), catalog.getName(), command.quantity());
            }
            return requestView(requests.save(request));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Transactional
    public EquipmentRequestView approve(CurrentActor actor, Long id, Map<Long, Integer> quantities, String comment) {
        requirePlatform(actor);
        EquipmentRequest request = findRequest(actor, id);
        try {
            request.approve(actor.userId(), comment, quantities);
            return requestView(requests.save(request));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Transactional
    public EquipmentRequestView reject(CurrentActor actor, Long id, String comment) {
        requirePlatform(actor);
        EquipmentRequest request = findRequest(actor, id);
        try {
            request.reject(actor.userId(), comment);
            return requestView(requests.save(request));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Transactional
    public EquipmentRequestView deliver(CurrentActor actor, Long id, Map<Long, Integer> quantities, String note) {
        requirePlatform(actor);
        EquipmentRequest request = findRequest(actor, id);
        try {
            request.deliver(quantities);
            int total = request.getItems().stream().mapToInt(EquipmentRequestItem::getDeliveredQuantity).sum();
            for (EquipmentRequestItem item : request.getItems()) {
                if (item.getDeliveredQuantity() <= 0) continue;
                EquipmentInventory inventory = inventories.findByTenantIdAndCatalogId(request.getTenantId(), item.getCatalogId())
                        .orElseGet(() -> EquipmentInventory.create(request.getTenantId(), item.getCatalogId()));
                inventory.registerDelivery(item.getDeliveredQuantity());
                inventories.save(inventory);
            }
            deliveries.save(EquipmentDelivery.create(request.getTenantId(), request.getId(), actor.userId(), total, note));
            return requestView(requests.save(request));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Transactional
    public EquipmentRequestView receive(CurrentActor actor, Long id) {
        requireGymAdmin(actor);
        EquipmentRequest request = findRequest(actor, id);
        try {
            request.receive();
            for (EquipmentRequestItem item : request.getItems()) {
                if (item.getReceivedQuantity() <= 0) continue;
                EquipmentInventory inventory = inventories.findByTenantIdAndCatalogId(request.getTenantId(), item.getCatalogId())
                        .orElseThrow(() -> new IllegalStateException("Inventory not found"));
                inventory.confirmReceipt(item.getReceivedQuantity());
                inventories.save(inventory);
            }
            return requestView(requests.save(request));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        } catch (IllegalStateException ex) {
            throw new BusinessException(ErrorCode.CONFLICT);
        }
    }

    @Transactional(readOnly = true)
    public List<EquipmentDeliveryView> listDeliveries(CurrentActor actor, Long tenantId) {
        requireEquipmentReader(actor);
        Long scope = resolveTenantScope(actor, tenantId);
        List<EquipmentDelivery> source = scope == null ? deliveries.findAll() : deliveries.findAllByTenantId(scope);
        return source.stream().map(item -> new EquipmentDeliveryView(item.getId(), item.getTenantId(), tenantName(item.getTenantId()), item.getRequestId(), item.getOperatorId(), item.getTotalQuantity(), item.getDeliveredAt(), item.getNote())).toList();
    }

    @Transactional(readOnly = true)
    public EquipmentStatisticsView statistics(CurrentActor actor, Long tenantId) {
        requireEquipmentReader(actor);
        Long scope = resolveTenantScope(actor, tenantId);
        List<EquipmentInventory> inventory = scope == null ? inventories.findAll() : inventories.findAllByTenantId(scope);
        List<EquipmentRequest> requestList = scope == null ? requests.findAll() : requests.findAllByTenantId(scope);
        Set<Long> tenantIds = inventory.stream().map(EquipmentInventory::getTenantId).collect(Collectors.toSet());
        long pending = requestList.stream().filter(item -> item.getStatus() == EquipmentRequestStatus.PENDING).count();
        long waiting = requestList.stream().filter(item -> item.getStatus() == EquipmentRequestStatus.WAITING_RECEIVE).count();
        List<EquipmentStatisticsView.EquipmentInventorySummaryView> summary = inventory.stream().map(item -> {
            EquipmentCatalog catalog = catalogs.findById(item.getCatalogId()).orElse(null);
            return new EquipmentStatisticsView.EquipmentInventorySummaryView(item.getTenantId(), tenantName(item.getTenantId()), catalog == null ? "未知器械" : catalog.getName(), item.getReceivedQuantity());
        }).toList();
        long catalogCount = scope == null ? catalogs.findAll().size() : inventory.stream().map(EquipmentInventory::getCatalogId).distinct().count();
        long visibleTenantCount = scope == null ? tenants.findAll(PageRequest.of(0, 10000)).getTotalElements() : 1;
        return new EquipmentStatisticsView(visibleTenantCount, catalogCount, inventory.stream().mapToInt(EquipmentInventory::getDeliveredQuantity).sum(), inventory.stream().mapToInt(EquipmentInventory::getReceivedQuantity).sum(), pending, waiting, summary);
    }

    private EquipmentRequest findRequest(CurrentActor actor, Long id) {
        if (ActorAccess.isSuperAdmin(actor)) return requests.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return requests.findByTenantIdAndId(ActorAccess.requireTenantId(actor), id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Long resolveTenantScope(CurrentActor actor, Long requestedTenantId) {
        if (ActorAccess.isSuperAdmin(actor)) return requestedTenantId;
        Long current = ActorAccess.requireTenantId(actor);
        if (requestedTenantId != null && !current.equals(requestedTenantId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        return current;
    }

    private EquipmentInventoryView inventoryView(EquipmentInventory inventory) {
        EquipmentCatalog catalog = catalogs.findById(inventory.getCatalogId()).orElse(null);
        return new EquipmentInventoryView(inventory.getTenantId(), tenantName(inventory.getTenantId()), inventory.getCatalogId(), catalog == null ? "未知器械" : catalog.getName(), catalog == null ? "" : catalog.getCategory(), catalog == null ? "台" : catalog.getUnit(), inventory.getDeliveredQuantity(), inventory.getReceivedQuantity(), inventory.getUsableQuantity(), inventory.getAbnormalQuantity(), inventory.getLastReceivedAt());
    }

    private EquipmentRequestView requestView(EquipmentRequest request) {
        return new EquipmentRequestView(request.getId(), request.getTenantId(), tenantName(request.getTenantId()), request.getRequesterId(), request.getStatus(), request.getNote(), request.getReviewerId(), request.getReviewedAt(), request.getReviewComment(), request.getItems().stream().map(item -> new EquipmentRequestItemView(item.getCatalogId(), item.getCatalogName(), item.getRequestedQuantity(), item.getApprovedQuantity(), item.getDeliveredQuantity(), item.getReceivedQuantity())).toList(), request.getCreatedAt());
    }

    private String tenantName(Long tenantId) { return tenants.findById(tenantId).map(t -> t.getName()).orElse("健身房 #" + tenantId); }
    private void requireEquipmentReader(CurrentActor actor) {
        if (actor == null) throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        if (!ActorAccess.isSuperAdmin(actor) && !actor.roles().contains(RoleCode.GYM_ADMIN)) throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    private void requirePlatform(CurrentActor actor) { if (actor == null || !ActorAccess.isSuperAdmin(actor)) throw new BusinessException(ErrorCode.FORBIDDEN); }
    private void requireGymAdmin(CurrentActor actor) { if (actor == null || !actor.roles().contains(RoleCode.GYM_ADMIN) || actor.tenantId() == null) throw new BusinessException(ErrorCode.FORBIDDEN); }
}
