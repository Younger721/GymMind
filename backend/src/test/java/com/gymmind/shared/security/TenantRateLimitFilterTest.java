package com.gymmind.shared.security;

import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRateLimitFilterTest {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestsFromOneTenantShareTheSameWindow() throws Exception {
        TenantRateLimitFilter filter = new TenantRateLimitFilter(2, Duration.ofMinutes(1), new ObjectMapper());
        install(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L, "token-7"));
        AtomicInteger passed = new AtomicInteger();

        MockHttpServletResponse first = invoke(filter, "/api/v1/members", passed);
        MockHttpServletResponse second = invoke(filter, "/api/v1/courses", passed);
        MockHttpServletResponse third = invoke(filter, "/api/v1/bookings", passed);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader("Retry-After")).isNotBlank();
        assertThat(passed).hasValue(2);
    }

    @Test
    void differentTenantsHaveIndependentWindows() throws Exception {
        TenantRateLimitFilter filter = new TenantRateLimitFilter(1, Duration.ofMinutes(1), new ObjectMapper());
        AtomicInteger passed = new AtomicInteger();

        install(actor(7L));
        MockHttpServletResponse tenantSeven = invoke(filter, "/api/v1/members", passed);
        MockHttpServletResponse tenantSevenBlocked = invoke(filter, "/api/v1/members", passed);

        install(actor(8L));
        MockHttpServletResponse tenantEight = invoke(filter, "/api/v1/members", passed);

        assertThat(tenantSeven.getStatus()).isEqualTo(200);
        assertThat(tenantSevenBlocked.getStatus()).isEqualTo(429);
        assertThat(tenantEight.getStatus()).isEqualTo(200);
        assertThat(passed).hasValue(2);
    }

    @Test
    void publicEndpointsAreNotRateLimited() throws Exception {
        TenantRateLimitFilter filter = new TenantRateLimitFilter(1, Duration.ofMinutes(1), new ObjectMapper());
        install(actor(7L));
        AtomicInteger passed = new AtomicInteger();

        MockHttpServletResponse first = invoke(filter, "/api/v1/auth/login", passed);
        MockHttpServletResponse second = invoke(filter, "/api/v1/auth/login", passed);

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(passed).hasValue(2);
    }

    private static CurrentActor actor(long tenantId) {
        return new CurrentActor(1L, tenantId, Set.of(RoleCode.GYM_ADMIN), Set.of(), 0L,
                "token-" + tenantId);
    }

    private static void install(CurrentActor actor) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(new GymMindPrincipal(actor), "", Set.of()));
    }

    private static MockHttpServletResponse invoke(
            TenantRateLimitFilter filter, String path, AtomicInteger passed) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                passed.incrementAndGet();
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200);
            }
        });
        return response;
    }
}
