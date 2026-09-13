package com.gymmind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PhaseOneSmokeIT {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.41")
            .withDatabaseName("gymmind")
            .withUsername("root")
            .withPassword("test-root-password");
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeAll
    static void verifyContainersStarted() {
        assertThat(MYSQL.isRunning()).isTrue();
        assertThat(REDIS.isRunning()).isTrue();
    }

    @AfterAll
    static void stopContainers() {
        REDIS.stop();
        MYSQL.stop();
    }

    @Test
    void twoTenantsRemainIsolatedAcrossFullAuthLifecycle() throws Exception {
        AuthTokens tenantA = register("tenant-a", "a@example.com");
        AuthTokens tenantB = register("tenant-b", "b@example.com");

        ResponseEntity<String> updated = exchange(HttpMethod.PUT, "/api/v1/tenant/settings", tenantA.accessToken(),
                Map.of("reservationEnabled", true, "checkInEnabled", true, "timezone", "Asia/Shanghai"));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> tenantBSettings = exchange(HttpMethod.GET, "/api/v1/tenant/settings",
                tenantB.accessToken(), null);
        assertThat(tenantBSettings.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(data(tenantBSettings).path("timezone").asText()).isEqualTo("UTC");

        ResponseEntity<String> tenantASettings = exchange(HttpMethod.GET, "/api/v1/tenant/settings",
                tenantA.accessToken(), null);
        assertThat(data(tenantASettings).path("timezone").asText()).isEqualTo("Asia/Shanghai");

        AuthTokens rotated = refresh(tenantA.refreshToken());
        ResponseEntity<String> logout = exchange(HttpMethod.POST, "/api/v1/auth/logout", rotated.accessToken(),
                Map.of("refreshToken", rotated.refreshToken()));
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> afterLogout = exchange(HttpMethod.GET, "/api/v1/tenant/settings",
                rotated.accessToken(), null);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private AuthTokens register(String tenantCode, String email) throws Exception {
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/v1/auth/register", null,
                Map.of("tenantCode", tenantCode, "tenantName", tenantCode,
                        "email", email, "password", "ValidPassword123!", "displayName", tenantCode));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode data = data(response);
        return new AuthTokens(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private AuthTokens refresh(String refreshToken) throws Exception {
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/api/v1/auth/refresh", null,
                Map.of("refreshToken", refreshToken));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = data(response);
        return new AuthTokens(data.path("accessToken").asText(), data.path("refreshToken").asText());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode data(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).path("data");
    }

    private record AuthTokens(String accessToken, String refreshToken) {
    }
}
