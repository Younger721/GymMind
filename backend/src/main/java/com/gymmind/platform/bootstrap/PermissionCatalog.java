package com.gymmind.platform.bootstrap;

import java.util.List;

public final class PermissionCatalog {
    private static final List<Entry> ENTRIES = List.of(
            new Entry("platform:tenant:read", "Read tenants"),
            new Entry("platform:tenant:write", "Manage tenants"),
            new Entry("tenant:settings:read", "Read tenant settings"),
            new Entry("tenant:settings:write", "Manage tenant settings"),
            new Entry("user:read", "Read users"),
            new Entry("user:write", "Manage users"),
            new Entry("role:read", "Read roles"),
            new Entry("role:assign", "Assign roles"),
            new Entry("member:read", "Read members"),
            new Entry("member:write", "Manage members"),
            new Entry("coach:read", "Read coaches"),
            new Entry("coach:write", "Manage coaches"),
            new Entry("coach:assign", "Assign coaches"),
            new Entry("course:read", "Read courses"),
            new Entry("course:write", "Manage courses"),
            new Entry("membership:read", "Read membership packages"),
            new Entry("membership:write", "Manage membership packages"),
            new Entry("payment:write", "Record offline payments"),
            new Entry("order:write", "Create orders"),
            new Entry("booking:write", "Create bookings"),
            new Entry("checkin:write", "Record check-ins"),
            new Entry("exercise:read", "Read exercises"),
            new Entry("exercise:write", "Manage exercises"),
            new Entry("video:read", "Read exercise videos"),
            new Entry("video:write", "Manage exercise videos"),
            new Entry("workout:read", "Read workout plans"),
            new Entry("workout:write", "Manage workout plans"));

    private PermissionCatalog() {}

    public static List<Entry> entries() { return ENTRIES; }

    public record Entry(String code, String name) {}
}
