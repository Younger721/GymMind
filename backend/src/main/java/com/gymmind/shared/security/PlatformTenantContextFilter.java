package com.gymmind.shared.security;

import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Establishes an explicit tenant context for platform-admin requests.
 *
 * A platform request must explicitly select an active tenant with a header.
 * Non-platform requests do not receive an implicit tenant context from this
 * filter.
 */
@Component
public class PlatformTenantContextFilter extends OncePerRequestFilter {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantRepository tenants;

    public PlatformTenantContextFilter(TenantRepository tenants) {
        this.tenants = Objects.requireNonNull(tenants, "tenants");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!resolvePlatformTenant(request, response)) {
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean resolvePlatformTenant(HttpServletRequest request, HttpServletResponse response) {
        String rawHeader = request.getHeader(TENANT_HEADER);
        if (rawHeader == null || rawHeader.isBlank()) {
            return true;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof GymMindPrincipal principal)
                || !principal.actor().isPlatformAdmin()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        Long tenantId = parseTenantId(rawHeader);
        if (tenantId == null || tenants.findById(tenantId).filter(tenant -> tenant.isActive()).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        TenantContextHolder.set(tenantId);
        return true;
    }

    private static Long parseTenantId(String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
