package com.gymmind.equipment.bootstrap;

import com.gymmind.equipment.application.DefaultEquipmentService;
import com.gymmind.equipment.application.EquipmentRequestView;
import com.gymmind.equipment.application.EquipmentService.RequestItemCommand;
import com.gymmind.equipment.domain.model.EquipmentCatalog;
import com.gymmind.equipment.domain.model.EquipmentInventory;
import com.gymmind.equipment.domain.model.EquipmentRequest;
import com.gymmind.equipment.domain.model.EquipmentRequestItem;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import com.gymmind.equipment.domain.model.PlatformEquipmentInventory;
import com.gymmind.equipment.domain.repository.EquipmentCatalogRepository;
import com.gymmind.equipment.domain.repository.EquipmentInventoryRepository;
import com.gymmind.equipment.domain.repository.EquipmentRequestRepository;
import com.gymmind.equipment.domain.repository.PlatformEquipmentInventoryRepository;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import com.gymmind.platform.bootstrap.DatabaseInitializationLock;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.model.TenantStatus;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 开发环境注入器械申请/配送演示数据，补全「申请记录」「配送记录」闭环展示。
 */
@Component
@Profile("dev")
@Order(350)
public class EquipmentDemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EquipmentDemoDataInitializer.class);
    /** 演示配送前保证平台仓库存不低于该值 */
    private static final int MIN_WAREHOUSE_STOCK = 500;

    private static final Set<String> PLATFORM_PERMS = Set.of(
            "equipment:catalog:read", "equipment:catalog:write", "equipment:inventory:read",
            "equipment:request:read", "equipment:request:write", "equipment:request:approve",
            "equipment:request:deliver", "equipment:request:receive", "equipment:stats:read");
    private static final Set<String> GYM_PERMS = Set.of(
            "equipment:catalog:read", "equipment:inventory:read",
            "equipment:request:read", "equipment:request:write", "equipment:request:receive",
            "equipment:stats:read");

    private final DatabaseInitializationLock initializationLock;
    private final EquipmentRequestRepository requests;
    private final EquipmentInventoryRepository inventories;
    private final EquipmentCatalogRepository catalogs;
    private final PlatformEquipmentInventoryRepository warehouse;
    private final DefaultEquipmentService equipmentService;
    private final UserAccountRepository users;
    private final UserRoleRepository userRoles;
    private final TenantRepository tenants;
    private final TransactionTemplate transactionTemplate;

    public EquipmentDemoDataInitializer(DatabaseInitializationLock initializationLock,
                                          EquipmentRequestRepository requests,
                                          EquipmentInventoryRepository inventories,
                                          EquipmentCatalogRepository catalogs,
                                          PlatformEquipmentInventoryRepository warehouse,
                                          DefaultEquipmentService equipmentService,
                                          UserAccountRepository users,
                                          UserRoleRepository userRoles,
                                          TenantRepository tenants,
                                          TransactionTemplate transactionTemplate) {
        this.initializationLock = initializationLock;
        this.requests = requests;
        this.inventories = inventories;
        this.catalogs = catalogs;
        this.warehouse = warehouse;
        this.equipmentService = equipmentService;
        this.users = users;
        this.userRoles = userRoles;
        this.tenants = tenants;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            initializationLock.execute("gymmind:equipment-demo-data",
                    () -> transactionTemplate.executeWithoutResult(status -> seedAndReconcile()));
        } catch (Exception ex) {
            // 演示数据失败不应阻断本地开发启动
            log.error("器械申请/配送演示数据写入失败，可清空 equipment_request 相关表后重启重试", ex);
        }
    }

    void seedAndReconcile() {
        ensureCatalogAndWarehouse();
        if (requests.findAll().isEmpty()) {
            inventories.deleteAll();
            Long platformAdminId = findPlatformAdminId();
            if (platformAdminId == null) {
                return;
            }
            CurrentActor platform = new CurrentActor(platformAdminId, null,
                    Set.of(RoleCode.PLATFORM_ADMIN), PLATFORM_PERMS, 0L, "equipment-demo-seed");
            List<Long> tenantIds = tenants.findAll(PageRequest.of(0, 5, Sort.by("id"))).stream()
                    .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                    .map(Tenant::getId)
                    .toList();
            for (Long tenantId : tenantIds) {
                Long gymAdminId = findGymAdminId(tenantId);
                if (gymAdminId == null) {
                    continue;
                }
                CurrentActor gym = new CurrentActor(gymAdminId, tenantId,
                        Set.of(RoleCode.GYM_ADMIN), GYM_PERMS, 0L, "equipment-demo-seed");
                seedForTenant(platform, gym);
            }
        }
        reconcileInventoryFromRequests();
    }

    /** 按申请单重建门店库存与平台仓，消除 SQL 种子库存造成的数量不对平 */
    void reconcileInventoryFromRequests() {
        List<EquipmentRequest> requestList = requests.findAll();
        if (requestList.isEmpty()) {
            return;
        }
        inventories.deleteAll();
        Map<Long, CatalogFlowTotals> platformTotals = new HashMap<>();
        requestList.stream()
                .sorted(Comparator.comparing(EquipmentRequest::getId))
                .forEach(request -> applyRequestToInventory(request, platformTotals));
        for (EquipmentCatalog catalog : catalogs.findAll()) {
            CatalogFlowTotals totals = platformTotals.getOrDefault(catalog.getId(), new CatalogFlowTotals());
            PlatformEquipmentInventory stock = warehouse.findByCatalogId(catalog.getId())
                    .orElseGet(() -> warehouse.save(PlatformEquipmentInventory.create(catalog.getId(), 0)));
            int warehouseQuantity = Math.max(0, MIN_WAREHOUSE_STOCK - totals.outbound);
            stock.reconcileFromRequestTotals(warehouseQuantity, totals.inTransit(), totals.outbound);
            warehouse.save(stock);
        }
        log.info("器械演示库存已按申请单对齐：{} 条申请参与汇总", requestList.size());
    }

    private void applyRequestToInventory(EquipmentRequest request, Map<Long, CatalogFlowTotals> platformTotals) {
        if (request.getStatus() != EquipmentRequestStatus.WAITING_RECEIVE
                && request.getStatus() != EquipmentRequestStatus.RECEIVED) {
            return;
        }
        for (EquipmentRequestItem item : request.getItems()) {
            int delivered = item.getDeliveredQuantity();
            if (delivered <= 0) {
                continue;
            }
            int received = item.getReceivedQuantity();
            EquipmentInventory inventory = inventories.findByTenantIdAndCatalogId(request.getTenantId(), item.getCatalogId())
                    .orElseGet(() -> inventories.save(EquipmentInventory.create(request.getTenantId(), item.getCatalogId())));
            inventory.registerDelivery(delivered);
            if (request.getStatus() == EquipmentRequestStatus.RECEIVED && received > 0) {
                inventory.confirmReceipt(received);
            }
            platformTotals.computeIfAbsent(item.getCatalogId(), key -> new CatalogFlowTotals())
                    .add(delivered, received);
        }
    }

    private static final class CatalogFlowTotals {
        int outbound;
        int receivedAtTenants;

        void add(int delivered, int received) {
            outbound += delivered;
            receivedAtTenants += received;
        }

        int inTransit() {
            return outbound - receivedAtTenants;
        }
    }

    private void seedForTenant(CurrentActor platform, CurrentActor gym) {
        Long treadmill = catalogId("跑步机");
        Long elliptical = catalogId("椭圆机");
        Long dumbbell = catalogId("哑铃");
        Long bike = catalogId("动感单车");
        if (treadmill == null || elliptical == null || dumbbell == null) {
            return;
        }

        // 待审批
        equipmentService.createRequest(gym, "演示数据：补充跑步机", List.of(new RequestItemCommand(treadmill, 2)));

        // 已驳回
        var rejected = equipmentService.createRequest(gym, "演示数据：临时加购（已驳回）",
                List.of(new RequestItemCommand(dumbbell, 50)));
        equipmentService.reject(platform, rejected.id(), "单次申请数量过大，请分批提交");

        // 已批准待配送
        Long bikeId = bike != null ? bike : elliptical;
        EquipmentRequestView approved = equipmentService.createRequest(gym, "演示数据：团课区增配动感单车",
                List.of(new RequestItemCommand(bikeId, 3)));
        equipmentService.approve(platform, approved.id(), Map.of(bikeId, 3), "批准 3 台");

        // 待门店确认入库
        var waiting = equipmentService.createRequest(gym, "演示数据：椭圆机更新",
                List.of(new RequestItemCommand(elliptical, 2)));
        equipmentService.approve(platform, waiting.id(), Map.of(elliptical, 2), "批准 2 台");
        equipmentService.deliver(platform, waiting.id(), Map.of(elliptical, 2), "演示配送：物流单 DEMO-001");

        // 已完成入库
        var done = equipmentService.createRequest(gym, "演示数据：哑铃套装补货",
                List.of(new RequestItemCommand(dumbbell, 4)));
        equipmentService.approve(platform, done.id(), Map.of(dumbbell, 4), "批准 4 套");
        equipmentService.deliver(platform, done.id(), Map.of(dumbbell, 4), "演示配送：已签收");
        equipmentService.receive(gym, done.id());
    }

    private Long catalogId(String name) {
        return catalogs.findAll().stream()
                .filter(item -> name.equals(item.getName()))
                .map(EquipmentCatalog::getId)
                .findFirst()
                .orElse(null);
    }

    private void ensureCatalogAndWarehouse() {
        if (catalogs.findAll().isEmpty()) {
            seedDefaultCatalog();
        }
        for (EquipmentCatalog catalog : catalogs.findAll()) {
            PlatformEquipmentInventory stock = warehouse.findByCatalogId(catalog.getId())
                    .orElseGet(() -> warehouse.save(PlatformEquipmentInventory.create(catalog.getId(), 0)));
            if (stock.getWarehouseQuantity() < MIN_WAREHOUSE_STOCK) {
                stock.adjust(MIN_WAREHOUSE_STOCK - stock.getWarehouseQuantity());
                warehouse.save(stock);
            }
        }
    }

    private void seedDefaultCatalog() {
        saveCatalog("跑步机", "有氧器械", "用于跑步和快走训练的电动有氧设备", "台");
        saveCatalog("动感单车", "有氧器械", "用于室内骑行和团体单车课程", "台");
        saveCatalog("椭圆机", "有氧器械", "低冲击全身有氧训练设备", "台");
        saveCatalog("划船机", "有氧器械", "用于模拟划船动作的全身训练设备", "台");
        saveCatalog("哑铃", "力量器械", "适用于上肢和全身力量训练", "套");
        saveCatalog("杠铃架", "力量器械", "用于深蹲、卧推等杠铃训练", "台");
    }

    private void saveCatalog(String name, String category, String description, String unit) {
        catalogs.save(EquipmentCatalog.create(name, category, description, unit));
    }

    private Long findPlatformAdminId() {
        for (Long userId : userRoles.findUserIdsByRoleCodes(Set.of(RoleCode.PLATFORM_ADMIN))) {
            Optional<UserAccount> account = users.findById(userId);
            if (account.isPresent() && account.get().getTenantId() == null) {
                return userId;
            }
        }
        return null;
    }

    private Long findGymAdminId(Long tenantId) {
        for (Long userId : userRoles.findUserIdsByRoleCodes(Set.of(RoleCode.GYM_ADMIN))) {
            Optional<UserAccount> account = users.findById(userId);
            if (account.isPresent() && tenantId.equals(account.get().getTenantId())) {
                return userId;
            }
        }
        return null;
    }
}
