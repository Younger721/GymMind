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
            new Entry("membership:write", "Manage membership packages"));

    private PermissionCatalog() {}

    public static List<Entry> entries() { return ENTRIES; }

    public record Entry(String code, String name) {}
}
