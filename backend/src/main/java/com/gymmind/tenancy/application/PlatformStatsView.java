package com.gymmind.tenancy.application;

public record PlatformStatsView(
        long totalTenants,
        long activeTenants,
        long totalAiCallsThisMonth,
        long tenantsWithAiModuleEnabled) {
}
