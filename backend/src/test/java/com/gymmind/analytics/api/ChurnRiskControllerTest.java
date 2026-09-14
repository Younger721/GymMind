package com.gymmind.analytics.api;

import com.gymmind.analytics.application.ChurnRiskService;
import com.gymmind.analytics.domain.ChurnRiskLevel;
import com.gymmind.analytics.domain.ChurnRiskResult;
import com.gymmind.shared.api.TraceIdFilter;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChurnRiskControllerTest {
    private ChurnRiskService service;
    private CurrentActorProvider actors;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ChurnRiskService.class);
        actors = mock(CurrentActorProvider.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ChurnRiskController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void assessesRiskForCurrentTenant() throws Exception {
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        when(service.assess(any(), any())).thenReturn(new ChurnRiskResult(82, ChurnRiskLevel.HIGH, java.util.List.of("长期未训练")));

        mockMvc.perform(post("/api/v1/analytics/churn-risks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daysSinceLastWorkout\":45,\"workoutsLast30Days\":1,\"workoutsPrevious30Days\":8,\"membershipDaysRemaining\":3,\"membershipExpiring\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(82))
                .andExpect(jsonPath("$.data.level").value("HIGH"));

        verify(service).assess(any(), any());
    }

    @Test
    void rejectsNegativeSignals() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/churn-risks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"daysSinceLastWorkout\":-1,\"workoutsLast30Days\":1,\"workoutsPrevious30Days\":1,\"membershipDaysRemaining\":1,\"membershipExpiring\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service, actors);
    }
}
