package com.gymmind.tenancy.api.request;

import com.gymmind.tenancy.application.command.CreateTenantCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 64) String tenantCode,
        @NotBlank @Size(max = 128) String tenantName,
        @NotBlank @Email @Size(max = 320) String adminEmail,
        @NotBlank @Size(min = 8, max = 128) String adminPassword,
        @NotBlank @Size(max = 128) String adminDisplayName) {

    public CreateTenantCommand toCommand() {
        return new CreateTenantCommand(tenantCode, tenantName, adminEmail, adminPassword, adminDisplayName);
    }
}
