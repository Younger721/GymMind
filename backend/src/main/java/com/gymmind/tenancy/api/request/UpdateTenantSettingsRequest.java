package com.gymmind.tenancy.api.request;

import com.gymmind.tenancy.application.command.UpdateTenantSettingsCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTenantSettingsRequest(
        @NotNull Boolean reservationEnabled,
        @NotNull Boolean checkInEnabled,
        @NotBlank @Size(max = 64) String timezone) {

    public UpdateTenantSettingsCommand toCommand() {
        return new UpdateTenantSettingsCommand(reservationEnabled, checkInEnabled, timezone);
    }
}
