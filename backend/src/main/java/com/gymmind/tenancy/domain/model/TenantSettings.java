package com.gymmind.tenancy.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sys_tenant_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_sys_tenant_settings_tenant_id", columnNames = "tenant_id"))
public class TenantSettings extends TenantScopedEntity {

    @Column(name = "reservation_enabled", nullable = false)
    private boolean reservationEnabled = true;

    @Column(name = "check_in_enabled", nullable = false)
    private boolean checkInEnabled = true;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    protected TenantSettings() {
    }

    public TenantSettings(Long tenantId) {
        super(tenantId);
    }

    public static TenantSettings create(Long tenantId) {
        return new TenantSettings(tenantId);
    }

    public boolean isReservationEnabled() {
        return reservationEnabled;
    }

    public void setReservationEnabled(boolean reservationEnabled) {
        this.reservationEnabled = reservationEnabled;
    }

    public boolean isCheckInEnabled() {
        return checkInEnabled;
    }

    public void setCheckInEnabled(boolean checkInEnabled) {
        this.checkInEnabled = checkInEnabled;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Timezone must not be blank");
        }
        this.timezone = timezone.trim();
    }
}
