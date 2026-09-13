package com.gymmind.coach.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "coach_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uk_coach_tenant_number", columnNames = {"tenant_id", "coach_number"}),
        @UniqueConstraint(name = "uk_coach_tenant_phone", columnNames = {"tenant_id", "phone"})
})
public class Coach extends TenantScopedEntity {
    @Column(name = "user_id") private Long userId;
    @Column(name = "coach_number", nullable = false, length = 64) private String coachNumber;
    @Column(nullable = false, length = 128) private String fullName;
    @Column(nullable = false, length = 32) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private CoachStatus status = CoachStatus.ACTIVE;

    protected Coach() {}
    private Coach(Long tenantId, Long userId, String coachNumber, String fullName, String phone) {
        super(requireTenant(tenantId)); this.userId = userId; this.coachNumber = required(coachNumber, "coachNumber");
        this.fullName = required(fullName, "fullName"); this.phone = required(phone, "phone");
    }
    public static Coach create(Long tenantId, Long userId, String coachNumber, String fullName, String phone) { return new Coach(tenantId, userId, coachNumber, fullName, phone); }
    public void update(String fullName, String phone) { this.fullName = required(fullName, "fullName"); this.phone = required(phone, "phone"); }
    public void suspend() { status = CoachStatus.SUSPENDED; }
    public Long getUserId() { return userId; }
    public String getCoachNumber() { return coachNumber; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public CoachStatus getStatus() { return status; }
    private static Long requireTenant(Long value) { if (value == null || value <= 0) throw new IllegalArgumentException("tenantId must be positive"); return value; }
    private static String required(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank"); return value.trim(); }
}
