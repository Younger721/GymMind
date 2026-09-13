package com.gymmind.tenancy.application.command;

public record CreateTenantCommand(
        String tenantCode,
        String tenantName,
        String adminEmail,
        String adminPassword,
        String adminDisplayName) {
}
