package com.gymmind.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(2)
public class TenantRateLimitFilter extends OncePerRequestFilter {

    private static final long CLEANUP_INTERVAL = 256;
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    private final int requestLimit;
    private final long windowMillis;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestsSeen = new AtomicLong();

    @Autowired
    public TenantRateLimitFilter(
            @Value("${gymmind.rate-limit.requests-per-minute:300}") int requestLimit,
            ObjectMapper objectMapper) {
        this(requestLimit, DEFAULT_WINDOW, objectMapper);
    }

    TenantRateLimitFilter(int requestLimit, Duration window, ObjectMapper objectMapper) {
        if (requestLimit <= 0 || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit and window must be positive");
        }
        this.requestLimit = requestLimit;
        this.windowMillis = window.toMillis();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(request.getContextPath() + "/api/v1/")
                || path.startsWith(request.getContextPath() + "/api/v1/auth/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CurrentActor actor = currentActor();
        String scope = scope(actor);
        if (scope == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        long windowStart = now / windowMillis * windowMillis;
        WindowCounter counter = counters.compute(scope, (key, current) -> {
            if (current == null || current.windowStartMillis != windowStart) {
                return new WindowCounter(windowStart, 1);
            }
            return new WindowCounter(windowStart, current.count + 1);
        });
        cleanupOccasionally(now);

        if (counter.count > requestLimit) {
            long retryAfterSeconds = Math.max(1, (windowStart + windowMillis - now + 999) / 1000);
            response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status().value());
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(
                    ErrorCode.TOO_MANY_REQUESTS.name(), ErrorCode.TOO_MANY_REQUESTS.message()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void cleanupOccasionally(long now) {
        if (requestsSeen.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            counters.entrySet().removeIf(entry -> entry.getValue().windowStartMillis + windowMillis <= now);
        }
    }

    private static CurrentActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof GymMindPrincipal principal) {
            return principal.actor();
        }
        return null;
    }

    private static String scope(CurrentActor actor) {
        if (actor == null) {
            return null;
        }
        return actor.tenantId() == null
                ? "platform:" + actor.userId()
                : "tenant:" + actor.tenantId();
    }

    private record WindowCounter(long windowStartMillis, int count) {
    }
}
