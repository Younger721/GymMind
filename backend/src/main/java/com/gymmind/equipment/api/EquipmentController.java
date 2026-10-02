package com.gymmind.equipment.api;

import com.gymmind.equipment.application.EquipmentCatalogView;
import com.gymmind.equipment.application.EquipmentInventoryView;
import com.gymmind.equipment.application.EquipmentRequestView;
import com.gymmind.equipment.application.EquipmentService;
import com.gymmind.equipment.application.EquipmentStatisticsView;
import com.gymmind.equipment.application.EquipmentDeliveryView;
import com.gymmind.equipment.domain.model.EquipmentRequestStatus;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/equipment")
public class EquipmentController {
    private final EquipmentService service;
    private final CurrentActorProvider actors;

    public EquipmentController(EquipmentService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping("/catalog")
    public ApiResponse<List<EquipmentCatalogView>> catalog(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.success(service.listCatalog(actors.requireCurrent(), activeOnly));
    }

    @PostMapping("/catalog")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EquipmentCatalogView> createCatalog(@Valid @RequestBody CatalogRequest request) {
        return ApiResponse.success(service.createCatalog(actors.requireCurrent(), request.name(), request.category(), request.description(), request.unit()));
    }

    @PatchMapping("/catalog/{id}")
    public ApiResponse<EquipmentCatalogView> updateCatalog(@PathVariable Long id, @Valid @RequestBody CatalogRequest request) {
        return ApiResponse.success(service.updateCatalog(actors.requireCurrent(), id, request.name(), request.category(), request.description(), request.unit(), request.active()));
    }

    @GetMapping("/inventory")
    public ApiResponse<List<EquipmentInventoryView>> inventory(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.success(service.listInventory(actors.requireCurrent(), tenantId));
    }

    @GetMapping("/requests")
    public ApiResponse<List<EquipmentRequestView>> requests(@RequestParam(required = false) Long tenantId,
                                                             @RequestParam(required = false) EquipmentRequestStatus status) {
        return ApiResponse.success(service.listRequests(actors.requireCurrent(), tenantId, status));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EquipmentRequestView> createRequest(@Valid @RequestBody CreateRequest request) {
        return ApiResponse.success(service.createRequest(actors.requireCurrent(), request.note(), request.items().stream().map(item -> new EquipmentService.RequestItemCommand(item.catalogId(), item.quantity())).toList()));
    }

    @PostMapping("/requests/{id}/approve")
    public ApiResponse<EquipmentRequestView> approve(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success(service.approve(actors.requireCurrent(), id, request.quantities(), request.comment()));
    }

    @PostMapping("/requests/{id}/reject")
    public ApiResponse<EquipmentRequestView> reject(@PathVariable Long id, @RequestBody(required = false) CommentRequest request) {
        return ApiResponse.success(service.reject(actors.requireCurrent(), id, request == null ? null : request.comment()));
    }

    @PostMapping("/requests/{id}/deliver")
    public ApiResponse<EquipmentRequestView> deliver(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return ApiResponse.success(service.deliver(actors.requireCurrent(), id, request.quantities(), request.comment()));
    }

    @PostMapping("/requests/{id}/receive")
    public ApiResponse<EquipmentRequestView> receive(@PathVariable Long id) {
        return ApiResponse.success(service.receive(actors.requireCurrent(), id));
    }

    @GetMapping("/deliveries")
    public ApiResponse<List<EquipmentDeliveryView>> deliveries(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.success(service.listDeliveries(actors.requireCurrent(), tenantId));
    }

    @GetMapping("/statistics")
    public ApiResponse<EquipmentStatisticsView> statistics(@RequestParam(required = false) Long tenantId) {
        return ApiResponse.success(service.statistics(actors.requireCurrent(), tenantId));
    }

    public record CatalogRequest(@NotBlank String name, @NotBlank String category, String description,
                                 String unit, boolean active) {
        public CatalogRequest(String name, String category, String description, String unit) {
            this(name, category, description, unit, true);
        }
    }

    public record CreateRequest(String note, @NotEmpty List<@Valid RequestItem> items) {}
    public record RequestItem(@NotNull Long catalogId, @Positive int quantity) {}
    public record DecisionRequest(@NotEmpty Map<@NotNull Long, @Positive Integer> quantities, String comment) {}
    public record CommentRequest(String comment) {}
}
