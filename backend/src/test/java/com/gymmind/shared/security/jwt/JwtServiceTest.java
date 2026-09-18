package com.gymmind.shared.security.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.config.SecurityProperties;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "GymMind";
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final String OTHER_SECRET = "abcdef0123456789abcdef0123456789";
    private static final Instant NOW = Instant.parse("2026-09-12T06:00:00Z");
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CurrentActor TENANT_ACTOR = new CurrentActor(
            42L,
            7L,
            Set.of(RoleCode.GYM_ADMIN, RoleCode.COACH),
            Set.of("tenant:settings:read", "user:write"),
            3L,
            "source-token-id");

    private MutableClock clock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(NOW);
        jwtService = service(ISSUER, SECRET, clock, "access-jti", "refresh-jti");
    }

    @Test
    void springConfigurationProvidesSingleInjectableJwtServiceWithRuntimeDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtTestConfiguration.class)
                .withPropertyValues(
                        "gymmind.security.jwt.issuer=" + ISSUER,
                        "gymmind.security.jwt.secret=" + SECRET,
                        "gymmind.security.jwt.access-ttl=PT15M",
                        "gymmind.security.jwt.refresh-ttl=P7D")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtService.class);
                    assertThat(context).hasSingleBean(Clock.class);
                    assertThat(context).hasSingleBean(TokenIdGenerator.class);

                    TokenPair pair = context.getBean(JwtService.class).issue(TENANT_ACTOR);
                    assertThat(pair.accessToken()).isNotBlank();
                    assertThat(pair.refreshToken()).isNotBlank();
                });
    }

    @Test
    void issuesAndVerifiesCompleteStronglyTypedClaimsWithIndependentTokenIds() {
        TokenPair pair = jwtService.issue(TENANT_ACTOR);

        JwtClaims access = jwtService.verify(pair.accessToken(), TokenType.ACCESS);
        JwtClaims refresh = jwtService.verify(pair.refreshToken(), TokenType.REFRESH);

        assertThat(pair.accessToken()).isNotEqualTo(pair.refreshToken());
        assertThat(pair.accessExpiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
        assertThat(pair.refreshExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(access.issuer()).isEqualTo(ISSUER);
        assertThat(access.userId()).isEqualTo(42L);
        assertThat(access.tenantId()).isEqualTo(7L);
        assertThat(access.roles()).containsExactlyInAnyOrder(RoleCode.GYM_ADMIN, RoleCode.COACH);
        assertThat(access.permissions()).containsExactlyInAnyOrder("tenant:settings:read", "user:write");
        assertThat(access.tokenVersion()).isEqualTo(3L);
        assertThat(access.tokenId()).isEqualTo("access-jti");
        assertThat(access.type()).isEqualTo(TokenType.ACCESS);
        assertThat(access.issuedAt()).isEqualTo(NOW);
        assertThat(access.expiresAt()).isEqualTo(NOW.plus(ACCESS_TTL));
        assertThat(refresh.tokenId()).isEqualTo("refresh-jti");
        assertThat(refresh.type()).isEqualTo(TokenType.REFRESH);
        assertThat(refresh.expiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(access.tokenId()).isNotEqualTo(refresh.tokenId());
    }

    @Test
    void preservesNullTenantForPlatformActor() {
        CurrentActor platform = new CurrentActor(
                1L,
                null,
                Set.of(RoleCode.PLATFORM_ADMIN),
                Set.of("platform:tenant:read"),
                0L,
                "source-platform-token");

        JwtClaims claims = jwtService.verify(jwtService.issue(platform).accessToken(), TokenType.ACCESS);

        assertThat(claims.tenantId()).isNull();
        assertThat(claims.roles()).containsExactly(RoleCode.PLATFORM_ADMIN);
    }

    @Test
    void rejectsRefreshTokenWhenAccessTokenExpected() {
        TokenPair pair = jwtService.issue(TENANT_ACTOR);

        assertUnauthenticated(pair.refreshToken(), TokenType.ACCESS);
    }

    @Test
    void rejectsExpiredTokenUsingInjectedClock() {
        TokenPair pair = jwtService.issue(TENANT_ACTOR);
        clock.advance(ACCESS_TTL.plusSeconds(1));

        assertUnauthenticated(pair.accessToken(), TokenType.ACCESS);
    }

    @Test
    void rejectsTokenAtExactExpirationBoundary() {
        TokenPair pair = jwtService.issue(TENANT_ACTOR);
        clock.advance(ACCESS_TTL);

        assertUnauthenticated(pair.accessToken(), TokenType.ACCESS);
    }

    @Test
    void rejectsWrongIssuerSignatureAndMalformedTokensWithoutLeakingRawValues() {
        String wrongIssuer = signedToken(claims -> claims.put("iss", "OtherIssuer"), SECRET);
        String wrongSignature = signedToken(claims -> {}, OTHER_SECRET);
        String malformed = "sensitive.raw.token-value";

        assertUnauthenticated(wrongIssuer, TokenType.ACCESS);
        assertUnauthenticated(wrongSignature, TokenType.ACCESS);
        assertUnauthenticated(malformed, TokenType.ACCESS);
        assertUnauthenticated(null, TokenType.ACCESS);
        assertUnauthenticated("  ", TokenType.ACCESS);
    }

    @Test
    void rejectsMissingOrInvalidStronglyTypedClaims() {
        List<String> invalidTokens = new ArrayList<>();
        invalidTokens.add(signedToken(claims -> claims.remove("sub"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("sub", "not-a-number"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("sub", "0"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("jti"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("jti", " "), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("iat"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("exp"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("iat", Date.from(NOW.plusSeconds(1))), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("exp", Date.from(NOW.minusSeconds(1))), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("roles"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("roles", List.of()), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("roles", List.of("UNKNOWN")), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("roles", "GYM_ADMIN"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("permissions"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("permissions", List.of(17)), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("tokenVersion"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("tokenVersion", -1), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("tokenVersion", "3"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.remove("type"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("type", "UNKNOWN"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("tenantId", "7"), SECRET));
        invalidTokens.add(signedToken(claims -> claims.put("tenantId", null), SECRET));
        invalidTokens.add(rawSignedToken(
                claims -> claims.put("iat", Long.toString(NOW.getEpochSecond())), SECRET));
        invalidTokens.add(rawSignedToken(
                claims -> claims.put("iat", NOW.getEpochSecond() + 0.5), SECRET));
        invalidTokens.add(rawSignedToken(
                claims -> claims.put("exp", Long.toString(NOW.plus(ACCESS_TTL).getEpochSecond())), SECRET));
        invalidTokens.add(rawSignedToken(
                claims -> claims.put("exp", NOW.plus(ACCESS_TTL).getEpochSecond() + 0.5), SECRET));
        invalidTokens.add(rawSignedToken(claims -> claims.put("iss", 17), SECRET));
        invalidTokens.add(rawSignedToken(claims -> claims.put("sub", 42), SECRET));
        invalidTokens.add(rawSignedToken(claims -> claims.put("jti", 19), SECRET));

        assertThat(invalidTokens).allSatisfy(rawToken ->
                assertUnauthenticated(rawToken, TokenType.ACCESS));
    }

    private JwtService service(String issuer, String secret, Clock serviceClock, String... tokenIds) {
        SecurityProperties properties = new SecurityProperties();
        properties.setJwt(new SecurityProperties.Jwt(issuer, secret, ACCESS_TTL, REFRESH_TTL));
        ArrayDeque<String> ids = new ArrayDeque<>(List.of(tokenIds));
        return new JjwtJwtService(properties, serviceClock, ids::removeFirst);
    }

    private String signedToken(Consumer<Map<String, Object>> mutation, String secret) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", "42");
        claims.put("jti", "crafted-jti");
        claims.put("iat", Date.from(NOW));
        claims.put("exp", Date.from(NOW.plus(ACCESS_TTL)));
        claims.put("tenantId", 7L);
        claims.put("roles", List.of("GYM_ADMIN"));
        claims.put("permissions", List.of("tenant:settings:read"));
        claims.put("tokenVersion", 3L);
        claims.put("type", "ACCESS");
        mutation.accept(claims);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().claims(claims).signWith(key).compact();
    }

    private String rawSignedToken(Consumer<Map<String, Object>> mutation, String secret) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER);
        claims.put("sub", "42");
        claims.put("jti", "crafted-jti");
        claims.put("iat", NOW.getEpochSecond());
        claims.put("exp", NOW.plus(ACCESS_TTL).getEpochSecond());
        claims.put("tenantId", 7L);
        claims.put("roles", List.of("GYM_ADMIN"));
        claims.put("permissions", List.of("tenant:settings:read"));
        claims.put("tokenVersion", 3L);
        claims.put("type", "ACCESS");
        mutation.accept(claims);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        try {
            byte[] payload = OBJECT_MAPPER.writeValueAsBytes(claims);
            return Jwts.builder().content(payload).signWith(key).compact();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not create test token", exception);
        }
    }

    private void assertUnauthenticated(String rawToken, TokenType expectedType) {
        assertThatThrownBy(() -> jwtService.verify(rawToken, expectedType))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED);
                    assertThat(exception.getMessage()).isEqualTo("未登录或登录已失效");
                    assertThat(exception.getCause()).isNull();
                    if (rawToken != null && !rawToken.isBlank()) {
                        assertThat(exception.toString()).doesNotContain(rawToken);
                    }
                });
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityProperties.class)
    @Import(JwtConfiguration.class)
    static class JwtTestConfiguration {
    }
}
