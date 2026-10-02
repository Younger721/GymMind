package com.gymmind.equipment.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "equipment_request")
public class EquipmentRequest extends TenantScopedEntity {
    @Column(name = "requester_id", nullable = false)
    private Long requesterId;
    @Column(length = 1000)
    private String note;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EquipmentRequestStatus status = EquipmentRequestStatus.PENDING;
    @Column(name = "reviewer_id")
    private Long reviewerId;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    @Column(name = "review_comment", length = 1000)
    private String reviewComment;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EquipmentRequestItem> items = new ArrayList<>();

    protected EquipmentRequest() {
    }

    private EquipmentRequest(Long tenantId, Long requesterId, String note) {
        super(tenantId);
        if (tenantId == null || requesterId == null || requesterId <= 0) {
            throw new IllegalArgumentException("Request tenant and requester are required");
        }
        this.requesterId = requesterId;
        this.note = note == null || note.isBlank() ? null : note.trim();
    }

    public static EquipmentRequest create(Long tenantId, Long requesterId, String note) {
        return new EquipmentRequest(tenantId, requesterId, note);
    }

    public EquipmentRequestItem addItem(Long catalogId, String catalogName, int quantity) {
        if (status != EquipmentRequestStatus.PENDING) throw new IllegalStateException("Request is not editable");
        if (items.stream().anyMatch(item -> item.getCatalogId().equals(catalogId))) {
            throw new IllegalArgumentException("Duplicate catalog item");
        }
        EquipmentRequestItem item = new EquipmentRequestItem(this, catalogId, catalogName, quantity);
        items.add(item);
        return item;
    }

    public void approve(Long reviewerId, String comment, Map<Long, Integer> quantities) {
        ensureStatus(EquipmentRequestStatus.PENDING);
        if (reviewerId == null || quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("Approval quantities are required");
        if (quantities.keySet().stream().anyMatch(key -> items.stream().noneMatch(item -> item.getCatalogId().equals(key)))) throw new IllegalArgumentException("Unknown catalog in approval");
        int total = 0;
        for (EquipmentRequestItem item : items) {
            int quantity = quantities.getOrDefault(item.getCatalogId(), 0);
            item.approve(quantity);
            total += quantity;
        }
        if (total <= 0) throw new IllegalArgumentException("At least one item must be approved");
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
        this.reviewComment = normalize(comment);
        this.status = EquipmentRequestStatus.APPROVED;
    }

    public void reject(Long reviewerId, String comment) {
        ensureStatus(EquipmentRequestStatus.PENDING);
        if (reviewerId == null) throw new IllegalArgumentException("Reviewer is required");
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
        this.reviewComment = normalize(comment);
        this.status = EquipmentRequestStatus.REJECTED;
    }

    public void deliver(Map<Long, Integer> quantities) {
        ensureStatus(EquipmentRequestStatus.APPROVED);
        if (quantities == null || quantities.isEmpty()) throw new IllegalArgumentException("Delivery quantities are required");
        if (quantities.keySet().stream().anyMatch(key -> items.stream().noneMatch(item -> item.getCatalogId().equals(key)))) throw new IllegalArgumentException("Unknown catalog in delivery");
        int total = 0;
        for (EquipmentRequestItem item : items) {
            int quantity = quantities.getOrDefault(item.getCatalogId(), 0);
            item.deliver(quantity);
            total += quantity;
        }
        if (total <= 0) throw new IllegalArgumentException("At least one item must be delivered");
        this.status = EquipmentRequestStatus.WAITING_RECEIVE;
    }

    public void receive() {
        if (status != EquipmentRequestStatus.WAITING_RECEIVE) throw new IllegalStateException("Request is not waiting for receipt");
        items.forEach(EquipmentRequestItem::receive);
        status = EquipmentRequestStatus.RECEIVED;
    }

    public void cancel() {
        if (status != EquipmentRequestStatus.PENDING) throw new IllegalStateException("Only pending request can be cancelled");
        status = EquipmentRequestStatus.CANCELLED;
    }

    private void ensureStatus(EquipmentRequestStatus expected) {
        if (status != expected) throw new IllegalStateException("Request status must be " + expected);
    }

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public Long getRequesterId() { return requesterId; }
    public String getNote() { return note; }
    public EquipmentRequestStatus getStatus() { return status; }
    public Long getReviewerId() { return reviewerId; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewComment() { return reviewComment; }
    public List<EquipmentRequestItem> getItems() { return List.copyOf(items); }
}
