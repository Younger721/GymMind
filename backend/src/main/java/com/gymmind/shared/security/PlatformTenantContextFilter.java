package com.gymmind.shared.security;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * 为平台管理员解析门店租户上下文（请求头 X-Tenant-Id 或首个活跃租户）。
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
            resolvePlatformTenant(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void resolvePlatformTenant(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof GymMindPrincipal principal)) {
            return;
        }
        CurrentActor actor = principal.actor();
        if (!actor.isPlatformAdmin()) {
            return;
        }

        Long tenantId = parseHeaderTenantId(request.getHeader(TENANT_HEADER));
        if (tenantId == null) {
            tenantId = tenants.findAll(PageRequest.of(0, 1))
                    .stream()
                    .filter(Tenant::isActive)
                    .map(Tenant::getId)
                    .findFirst()
                    .orElse(null);
        }
        if (tenantId != null) {
            TenantContextHolder.set(tenantId);
        }
    }

    private static Long parseHeaderTenantId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            long tenantId = Long.parseLong(raw.trim());
            return tenantId > 0 ? tenantId : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
