package com.gymmind.shared.security.jwt;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.config.SecurityProperties;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class JjwtJwtService implements JwtService {

    private static final String TENANT_ID = "tenantId";
    private static final String ROLES = "roles";
    private static final String PERMISSIONS = "permissions";
    private static final String TOKEN_VERSION = "tokenVersion";
    private static final String TYPE = "type";
    private static final ObjectMapper CLAIMS_JSON = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private final String issuer;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecretKey signingKey;
    private final Clock clock;
    private final TokenIdGenerator tokenIdGenerator;

    public JjwtJwtService(
            SecurityProperties securityProperties,
            Clock clock,
            TokenIdGenerator tokenIdGenerator) {
        Objects.requireNonNull(securityProperties, "securityProperties");
        SecurityProperties.Jwt jwt = Objects.requireNonNull(securityProperties.getJwt(), "jwt");
        jwt.validate();
        this.issuer = requireText(jwt.issuer(), "Issuer");
        this.accessTtl = requirePositive(jwt.accessTtl(), "Access TTL");
        this.refreshTtl = requirePositive(jwt.refreshTtl(), "Refresh TTL");
        this.signingKey = Keys.hmacShaKeyFor(jwt.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tokenIdGenerator = Objects.requireNonNull(tokenIdGenerator, "tokenIdGenerator");
    }

    @Override
    public TokenPair issue(CurrentActor actor) {
        Objects.requireNonNull(actor, "actor");
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant accessExpiresAt = issuedAt.plus(accessTtl);
        Instant refreshExpiresAt = issuedAt.plus(refreshTtl);
        String accessTokenId = nextTokenId();
        String refreshTokenId = nextTokenId();
        if (accessTokenId.equals(refreshTokenId)) {
            throw new IllegalStateException("Access and refresh token ids must differ");
        }
        String accessToken = buildToken(
                actor, TokenType.ACCESS, accessTokenId, issuedAt, accessExpiresAt);
        String refreshToken = buildToken(
                actor, TokenType.REFRESH, refreshTokenId, issuedAt, refreshExpiresAt);
        return new TokenPair(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt);
    }

    @Override
    public JwtClaims verify(String rawToken, TokenType expectedType) {
        try {
            if (rawToken == null || rawToken.isBlank() || expectedType == null) {
                throw new IllegalArgumentException("Token and expected type are required");
            }
            Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(rawToken);
            JwtClaims parsed = mapClaims(parseRawClaims(rawToken));
            Instant now = clock.instant();
            if (parsed.type() != expectedType
                    || parsed.issuedAt().isAfter(now)
                    || !parsed.expiresAt().isAfter(now)) {
                throw new IllegalArgumentException("Invalid token claims");
            }
            return parsed;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String buildToken(
            CurrentActor actor,
            TokenType type,
            String tokenId,
            Instant issuedAt,
            Instant expiresAt) {
        var builder = Jwts.builder()
                .issuer(issuer)
                .subject(actor.userId().toString())
                .id(tokenId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(ROLES, actor.roles().stream().map(RoleCode::name).sorted().toList())
                .claim(PERMISSIONS, actor.permissions().stream().sorted().toList())
                .claim(TOKEN_VERSION, actor.tokenVersion())
                .claim(TYPE, type.name());
        if (actor.tenantId() != null) {
            builder.claim(TENANT_ID, actor.tenantId());
        }
        return builder.signWith(signingKey).compact();
    }

    private JwtClaims mapClaims(Map<String, Object> claims) {
        Instant issuedAt = Instant.ofEpochSecond(parseIntegralNumber(claims.get("iat"), "Issue time"));
        Instant expiresAt = Instant.ofEpochSecond(parseIntegralNumber(claims.get("exp"), "Expiration"));
        return new JwtClaims(
                requireText(requireString(claims.get("iss"), "Issuer"), "Issuer"),
                parsePositiveLong(requireString(claims.get("sub"), "Subject"), "Subject"),
                parseOptionalLongClaim(claims.get(TENANT_ID), "Tenant id"),
                parseRoles(claims.get(ROLES)),
                parsePermissions(claims.get(PERMISSIONS)),
                parseNonNegativeLongClaim(claims.get(TOKEN_VERSION), "Token version"),
                requireText(requireString(claims.get("jti"), "Token id"), "Token id"),
                TokenType.valueOf(requireText(requireString(claims.get(TYPE), "Token type"), "Token type")),
                issuedAt,
                expiresAt);
    }

    private Map<String, Object> parseRawClaims(String rawToken) throws Exception {
        String[] segments = rawToken.split("\\.", -1);
        if (segments.length != 3) {
            throw new IllegalArgumentException("Token must have three segments");
        }
        byte[] payload = Base64.getUrlDecoder().decode(segments[1]);
        return CLAIMS_JSON.readValue(payload, new TypeReference<>() {});
    }

    private Set<RoleCode> parseRoles(Object value) {
        if (!(value instanceof List<?> values) || values.isEmpty()) {
            throw new IllegalArgumentException("Roles must be a non-empty list");
        }
        return values.stream()
                .map(role -> RoleCode.valueOf(requireString(role, "Role")))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> parsePermissions(Object value) {
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Permissions must be a list");
        }
        return values.stream()
                .map(permission -> requireText(requireString(permission, "Permission"), "Permission"))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Long parseOptionalLongClaim(Object value, String label) {
        return value == null ? null : parseIntegralNumber(value, label);
    }

    private long parseNonNegativeLongClaim(Object value, String label) {
        long parsed = parseIntegralNumber(value, label);
        if (parsed < 0) {
            throw new IllegalArgumentException(label + " must not be negative");
        }
        return parsed;
    }

    private long parseIntegralNumber(Object value, String label) {
        if (!(value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long)) {
            throw new IllegalArgumentException(label + " must be an integer");
        }
        return ((Number) value).longValue();
    }

    private Long parsePositiveLong(String value, String label) {
        long parsed = Long.parseLong(requireText(value, label));
        if (parsed <= 0) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return parsed;
    }

    private String nextTokenId() {
        return requireText(tokenIdGenerator.generate(), "Token id");
    }

    private static String requireString(Object value, String label) {
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException(label + " must be a string");
        }
        return string;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    private static Duration requirePositive(Duration value, String label) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(label + " must be positive");
        }
        return value;
    }
}
