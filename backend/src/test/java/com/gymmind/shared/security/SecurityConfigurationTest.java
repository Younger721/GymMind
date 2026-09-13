package com.gymmind.shared.security;

import com.gymmind.shared.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityConfigurationTest.SecurityProbeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityConfigurationTest.SecurityProbeController.class
})
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PrincipalLoader principalLoader;

    @Test
    void onlyAuthenticationEntryPointsHealthAndOpenApiArePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/test-secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void authenticatedRequestsUsePermissionAuthoritiesAndUnified403() throws Exception {
        mockMvc.perform(get("/api/v1/test-secured").with(user("member")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/test-permission").with(user("member")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/test-permission")
                        .with(user("admin").authorities(() -> "tenant:settings:write")))
                .andExpect(status().isOk());
    }

    @Test
    void csrfIsDisabledForStatelessJsonApi() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isOk());
    }

    @RestController
    static class SecurityProbeController {

        @PostMapping({
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout"
        })
        String authenticationOperation() {
            return "ok";
        }

        @GetMapping({
                "/actuator/health",
                "/v3/api-docs",
                "/swagger-ui/index.html",
                "/api/v1/test-secured"
        })
        String readableOperation() {
            return "ok";
        }

        @GetMapping("/api/v1/test-permission")
        @PreAuthorize("hasAuthority('tenant:settings:write')")
        String permissionOperation() {
            return "ok";
        }
    }
}
