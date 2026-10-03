package com.gymmind.workout.api;

import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.workout.application.TrainingOverviewService;
import com.gymmind.workout.application.TrainingPlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingControllerTest {
    private TrainingOverviewService overviewService;
    private CurrentActorProvider actors;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        overviewService = mock(TrainingOverviewService.class);
        TrainingPlanService plans = mock(TrainingPlanService.class);
        actors = mock(CurrentActorProvider.class);
        mvc = MockMvcBuilders.standaloneSetup(new TrainingController(overviewService, plans, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bindsIsoDatesForOverview() throws Exception {
        mvc.perform(get("/api/v1/training/overview")
                        .param("since", "2026-09-04")
                        .param("until", "2026-10-03"))
                .andExpect(status().isOk());

        verify(overviewService).overview(eq(null), eq(LocalDate.of(2026, 9, 4)), eq(LocalDate.of(2026, 10, 3)), eq(null));
    }
}
