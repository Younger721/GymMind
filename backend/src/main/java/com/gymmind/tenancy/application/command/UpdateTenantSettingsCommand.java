package com.gymmind.tenancy.application.command;

public record UpdateTenantSettingsCommand(
        Boolean reservationEnabled,
        Boolean checkInEnabled,
        String timezone) {
}
