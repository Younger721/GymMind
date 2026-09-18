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
            new Entry("workout:write", "Manage workout plans"),
            new Entry("workout-record:read", "Read workout records"),
            new Entry("workout-record:write", "Create workout records"),
            new Entry("nutrition:read", "Read nutrition data"),
            new Entry("nutrition:write", "Manage nutrition data"),
            new Entry("ai:chat", "Use the AI assistant"),
            new Entry("agent:read", "List tenant agents"),
            new Entry("agent:write", "Manage tenant agents"),
            new Entry("agent:chat", "Chat with tenant agents"),
            new Entry("tenant:quota:read", "Read tenant quota usage"),
            new Entry("platform:quota:read", "Read tenant quotas"),
            new Entry("platform:quota:write", "Manage tenant quotas"),
            new Entry("platform:stats:read", "Read platform statistics"),
            new Entry("platform:knowledge:read", "Read all tenant knowledge documents"),
            new Entry("platform:agent:read", "Read all tenant agents"),
            new Entry("search:read", "Search imported articles"),
            new Entry("search:write", "Import public articles"),
            new Entry("ai:memory", "Manage personal AI memory"),
            new Entry("ai:recommend", "Use personalized recommendations"),
            new Entry("ai:report", "View AI generated reports"),
            new Entry("audit:read", "Read audit records"),
            new Entry("knowledge:write", "Manage knowledge documents"),
            new Entry("ai:plan", "Generate workout plan drafts"),
            new Entry("ai:analysis", "Use AI member and operation analysis"),
            new Entry("analytics:read", "Read operational analytics"),
            new Entry("knowledge:read", "Search knowledge documents"));

    private PermissionCatalog() {}

    public static List<Entry> entries() { return ENTRIES; }

    public record Entry(String code, String name) {}
}
