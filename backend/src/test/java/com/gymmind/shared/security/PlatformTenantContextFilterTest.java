package com.gymmind.shared.security;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformTenantContextFilterTest {

    @AfterEach
    void clearSecurityAndTenantContext() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void platformRequestWithoutTenantHeaderUsesFirstActiveTenant() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        Tenant tenant = Tenant.create("gym-east", "Gym East");
        ReflectionTestUtils.setField(tenant, "id", 7L);
        when(tenants.findAll(any())).thenReturn(new PageImpl<>(java.util.List.of(tenant), PageRequest.of(0, 1), 1));
        PlatformTenantContextFilter filter = new PlatformTenantContextFilter(tenants);
        installPlatformAuthentication();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request,
                                 jakarta.servlet.ServletResponse response) {
                assertThat(TenantContextHolder.get()).isEqualTo(7L);
            }
        };
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"),
                new MockHttpServletResponse(), chain);

        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void platformRequestWithActiveTenantHeaderCreatesTenantContext() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        Tenant tenant = Tenant.create("gym-east", "Gym East");
        ReflectionTestUtils.setField(tenant, "id", 7L);
        when(tenants.findById(7L)).thenReturn(Optional.of(tenant));
        PlatformTenantContextFilter filter = new PlatformTenantContextFilter(tenants);
        installPlatformAuthentication();

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request,
                                 jakarta.servlet.ServletResponse response) {
                assertThat(TenantContextHolder.get()).isEqualTo(7L);
            }
        };
        filter.doFilter(requestWithTenant("7"), new MockHttpServletResponse(), chain);

        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void nonPlatformRequestWithoutTenantHeaderDoesNotUseFirstActiveTenant() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        PlatformTenantContextFilter filter = new PlatformTenantContextFilter(tenants);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new GymMindPrincipal(com.gymmind.support.SecurityTestActors.gymAdmin()), "",
                        java.util.List.of()));

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request,
                                 jakarta.servlet.ServletResponse response) {
                assertThat(TenantContextHolder.get()).isNull();
            }
        };
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"),
                new MockHttpServletResponse(), chain);

        verifyNoInteractions(tenants);
    }

    @Test
    void platformRequestWithUnknownTenantHeaderIsForbidden() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        when(tenants.findById(7L)).thenReturn(Optional.empty());
        PlatformTenantContextFilter filter = new PlatformTenantContextFilter(tenants);
        installPlatformAuthentication();

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestWithTenant("7"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void nonPlatformRequestCannotSetTenantContextHeader() throws Exception {
        TenantRepository tenants = mock(TenantRepository.class);
        PlatformTenantContextFilter filter = new PlatformTenantContextFilter(tenants);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new GymMindPrincipal(com.gymmind.support.SecurityTestActors.gymAdmin()), "",
                        java.util.List.of()));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(requestWithTenant("7"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        verifyNoInteractions(tenants);
    }

    private static MockHttpServletRequest requestWithTenant(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users");
        request.addHeader("X-Tenant-Id", tenantId);
        return request;
    }

    private static void installPlatformAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        new GymMindPrincipal(platformAdmin()), "", java.util.List.of()));
    }
}
