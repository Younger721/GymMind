package com.gymmind.platform.bootstrap;

import com.gymmind.iam.domain.model.RoleCode;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The permission catalog has two layers: menu permissions are the public role
 * configuration surface, while operation permissions are used by existing
 * application services. Saving a menu selection expands it to its operations.
 */
public final class PermissionCatalog {
    /** Internal migration marker; never exposed as a menu or operation choice. */
    public static final String MENU_CONFIGURED_MARKER = "role:menu:configured";
    public static final String MENU_CONFIGURED_MARKER_NAME = "Role menu configuration marker";

    private static final List<Entry> ENTRIES = List.of(
            new Entry("platform:tenant:read", "Read tenants"),
            new Entry("platform:tenant:write", "Manage tenants"),
            new Entry("tenant:settings:read", "Read tenant settings"),
            new Entry("tenant:settings:write", "Manage tenant settings"),
            new Entry("user:read", "Read users"),
            new Entry("user:write", "Manage users"),
            new Entry("role:read", "Read roles"),
            new Entry("role:assign", "Assign roles"),
            new Entry("role:write", "Manage role status"),
            new Entry("role:permission:read", "Read role menu permissions"),
            new Entry("role:permission:write", "Manage role menu permissions"),
            new Entry("member:read", "Read members"),
            new Entry("member:write", "Manage members"),
            new Entry("coach:read", "Read coaches"),
            new Entry("coach:write", "Manage coaches"),
            new Entry("coach:assign", "Assign coaches"),
            new Entry("coach-relation:request", "Request a coach relationship"),
            new Entry("coach-relation:read", "Read coach relationships"),
            new Entry("coach-relation:accept", "Accept coach requests"),
            new Entry("coach-relation:reject", "Reject coach requests"),
            new Entry("coach-relation:manage", "Manage coach relationships"),
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
            new Entry("ai:memory", "Manage personal AI memory"),
            new Entry("ai:recommend", "Use personalized recommendations"),
            new Entry("ai:plan", "Generate workout plan drafts"),
            new Entry("ai:report", "View AI generated reports"),
            new Entry("ai:analysis", "Use AI member and operation analysis"),
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
            new Entry("audit:read", "Read audit records"),
            new Entry("knowledge:read", "Search knowledge documents"),
            new Entry("knowledge:write", "Manage knowledge documents"),
            new Entry("analytics:read", "Read operational analytics"));

    private static final List<MenuEntry> MENU_ENTRIES = List.of(
            new MenuEntry("menu:dashboard", "Dashboard"),
            new MenuEntry("menu:assistant", "AI assistant"),
            new MenuEntry("menu:knowledge", "Knowledge base"),
            new MenuEntry("menu:operations", "Gym operations"),
            new MenuEntry("menu:courses", "Courses"),
            new MenuEntry("menu:training", "Training"),
            new MenuEntry("menu:analytics", "Risk and analytics"),
            new MenuEntry("menu:admin", "Users and roles"),
            new MenuEntry("menu:tenant-settings", "Tenant settings"),
            new MenuEntry("menu:platform", "Platform console"),
            new MenuEntry("menu:role-permissions", "Role permissions"),
            new MenuEntry("menu:profile", "Account settings"));

    private static final Map<String, Set<String>> MENU_OPERATIONS = menuOperations();
    private static final Map<String, String> OPERATION_MENUS = operationMenus();
    private static final Map<RoleCode, Set<String>> DEFAULT_MENUS = defaults();
    private static final Map<RoleCode, Map<String, Set<String>>> ROLE_MENU_OPERATIONS = roleMenuOperations();

    static {
        Set<String> catalogCodes = ENTRIES.stream().map(Entry::code).collect(Collectors.toSet());
        Set<String> mappedCodes = OPERATION_MENUS.keySet();
        if (!catalogCodes.equals(mappedCodes)) {
            Set<String> unmapped = new LinkedHashSet<>(catalogCodes);
            unmapped.removeAll(mappedCodes);
            Set<String> unknown = new LinkedHashSet<>(mappedCodes);
            unknown.removeAll(catalogCodes);
            throw new IllegalStateException("Permission catalog mapping is incomplete; unmapped="
                    + unmapped + ", unknown=" + unknown);
        }
    }

    private PermissionCatalog() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static List<MenuEntry> menuEntries() {
        return MENU_ENTRIES;
    }

    public static boolean isMenuPermission(String code) {
        return code != null && MENU_OPERATIONS.containsKey(code);
    }

    public static Optional<String> menuPermissionFor(String operationCode) {
        return Optional.ofNullable(OPERATION_MENUS.get(operationCode));
    }

    public static Set<String> operationPermissionsFor(String menuCode) {
        return MENU_OPERATIONS.getOrDefault(menuCode, Set.of());
    }

    /**
     * Returns the operation permissions that a role may use within a menu.
     * Menu visibility is the configuration boundary, but individual roles do
     * not necessarily use every operation exposed by the shared menu.
     */
    public static Set<String> operationPermissionsFor(RoleCode roleCode, String menuCode) {
        if (roleCode == RoleCode.PLATFORM_ADMIN || roleCode == RoleCode.GYM_ADMIN) {
            return operationPermissionsFor(menuCode);
        }
        if (roleCode == null || !isMenuAllowedForRole(roleCode, menuCode)) {
            return Set.of();
        }
        return ROLE_MENU_OPERATIONS.getOrDefault(roleCode, Map.of())
                .getOrDefault(menuCode, Set.of());
    }

    public static Set<String> defaultMenus(RoleCode roleCode) {
        if (roleCode == RoleCode.PLATFORM_ADMIN) {
            return MENU_ENTRIES.stream().map(MenuEntry::code).collect(Collectors.toUnmodifiableSet());
        }
        return DEFAULT_MENUS.getOrDefault(roleCode, Set.of());
    }

    /**
     * Returns the menu boundary that a platform administrator may configure
     * for the role. Defaults are also the supported business boundary: a
     * hidden menu may be removed, but a role cannot be granted another
     * role's administrative or platform menu.
     */
    public static boolean isMenuAllowedForRole(RoleCode roleCode, String menuCode) {
        return roleCode != null && defaultMenus(roleCode).contains(menuCode);
    }

    private static Map<String, Set<String>> menuOperations() {
        Map<String, Set<String>> mappings = new LinkedHashMap<>();
        mappings.put("menu:dashboard", Set.of());
        mappings.put("menu:assistant", Set.of(
                "ai:chat", "ai:memory", "agent:read", "agent:write", "agent:chat"));
        mappings.put("menu:knowledge", Set.of(
                "search:read", "search:write", "knowledge:read", "knowledge:write"));
            mappings.put("menu:operations", Set.of(
                "member:read", "member:write", "coach:read", "coach:write", "coach:assign",
                "coach-relation:request", "coach-relation:read", "coach-relation:accept",
                "coach-relation:reject", "coach-relation:manage", "membership:read", "membership:write",
                "payment:write", "order:write", "checkin:write"));
        mappings.put("menu:courses", Set.of("course:read", "course:write", "booking:write"));
        mappings.put("menu:training", Set.of(
                "exercise:read", "exercise:write", "video:read", "video:write", "workout:read",
                "workout:write", "workout-record:read", "workout-record:write", "nutrition:read",
                "nutrition:write", "ai:recommend", "ai:plan"));
        mappings.put("menu:analytics", Set.of("analytics:read", "ai:report", "ai:analysis"));
        mappings.put("menu:admin", Set.of(
                "user:read", "user:write", "role:read", "role:assign", "role:write", "audit:read"));
        mappings.put("menu:tenant-settings", Set.of(
                "tenant:settings:read", "tenant:settings:write", "tenant:quota:read"));
        mappings.put("menu:platform", Set.of(
                "platform:tenant:read", "platform:tenant:write", "platform:quota:read",
                "platform:quota:write", "platform:stats:read", "platform:knowledge:read",
                "platform:agent:read"));
        mappings.put("menu:role-permissions", Set.of(
                "role:permission:read", "role:permission:write"));
        mappings.put("menu:profile", Set.of());
        return mappings.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private static Map<String, String> operationMenus() {
        Map<String, String> reverse = new LinkedHashMap<>();
        MENU_OPERATIONS.forEach((menu, operations) -> operations.forEach(operation -> {
            String previous = reverse.put(operation, menu);
            if (previous != null) {
                throw new IllegalStateException("Operation permission mapped to multiple menus: " + operation);
            }
        }));
        return Map.copyOf(reverse);
    }

    private static Map<RoleCode, Set<String>> defaults() {
        Map<RoleCode, Set<String>> defaults = new EnumMap<>(RoleCode.class);
        defaults.put(RoleCode.GYM_ADMIN, Set.of(
                "menu:dashboard", "menu:assistant", "menu:knowledge", "menu:operations", "menu:courses",
                "menu:training", "menu:analytics", "menu:admin", "menu:tenant-settings", "menu:profile"));
        defaults.put(RoleCode.COACH, Set.of(
                "menu:dashboard", "menu:assistant", "menu:knowledge", "menu:courses", "menu:training", "menu:profile"));
        defaults.put(RoleCode.MEMBER, Set.of(
                "menu:dashboard", "menu:assistant", "menu:courses", "menu:training", "menu:profile"));
        return defaults.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private static Map<RoleCode, Map<String, Set<String>>> roleMenuOperations() {
        Map<RoleCode, Map<String, Set<String>>> mappings = new EnumMap<>(RoleCode.class);
        mappings.put(RoleCode.COACH, roleOperations(
                Map.entry("menu:assistant", Set.of("ai:chat", "ai:memory", "agent:read", "agent:chat", "ai:plan")),
                Map.entry("menu:knowledge", Set.of("knowledge:read", "knowledge:write")),
                Map.entry("menu:courses", Set.of("course:read", "course:write", "booking:write")),
                Map.entry("menu:training", Set.of(
                        "exercise:read", "video:read", "workout:read", "workout:write",
                        "workout-record:read", "nutrition:read", "nutrition:write",
                        "ai:recommend", "ai:plan"))));
        mappings.put(RoleCode.MEMBER, roleOperations(
                Map.entry("menu:assistant", Set.of("ai:chat", "ai:memory", "ai:plan")),
                Map.entry("menu:courses", Set.of("course:read", "booking:write")),
                Map.entry("menu:training", Set.of(
                        "exercise:read", "video:read", "workout:read", "workout-record:read",
                        "workout-record:write", "nutrition:read", "nutrition:write",
                        "ai:recommend", "ai:plan"))));
        return mappings.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().entrySet().stream()
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, value -> Set.copyOf(value.getValue())))));
    }

    @SafeVarargs
    private static Map<String, Set<String>> roleOperations(Map.Entry<String, Set<String>>... entries) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : entries) {
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return result;
    }

    public record Entry(String code, String name) {}

    public record MenuEntry(String code, String name) {}
}
