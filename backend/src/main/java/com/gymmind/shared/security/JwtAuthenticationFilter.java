package com.gymmind.shared.security;

import com.gymmind.shared.security.jwt.JwtClaims;
import com.gymmind.shared.security.jwt.JwtService;
import com.gymmind.shared.security.jwt.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final PrincipalLoader principalLoader;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PrincipalLoader principalLoader,
            RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = Objects.requireNonNull(jwtService, "jwtService");
        this.principalLoader = Objects.requireNonNull(principalLoader, "principalLoader");
        this.authenticationEntryPoint = Objects.requireNonNull(authenticationEntryPoint, "authenticationEntryPoint");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean installedAuthentication = false;
        try {
            String rawToken = bearerToken(authorization);
            JwtClaims claims = jwtService.verify(rawToken, TokenType.ACCESS);
            if (claims.type() != TokenType.ACCESS) {
                throw new BadCredentialsException("Unexpected token type");
            }
            CurrentActor actor = principalLoader.load(claims);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    new GymMindPrincipal(actor), "", authorities(actor));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            installedAuthentication = true;
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request, response, new BadCredentialsException("Invalid bearer token"));
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (installedAuthentication) {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private static String bearerToken(String authorization) {
        if (authorization.length() <= BEARER_PREFIX.length()
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BadCredentialsException("Malformed authorization header");
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) {
            throw new BadCredentialsException("Malformed bearer token");
        }
        return token;
    }

    private static java.util.List<GrantedAuthority> authorities(CurrentActor actor) {
        return Stream.concat(
                        actor.roles().stream().map(role -> "ROLE_" + role.name()),
                        actor.permissions().stream())
                .distinct()
                .sorted()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
