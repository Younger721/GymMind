package com.gymmind.course.api;

import com.gymmind.course.application.CourseService;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseControllerTest {
    private CourseService service;
    private CurrentActorProvider actors;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CourseService.class);
        actors = mock(CurrentActorProvider.class);
        mvc = MockMvcBuilders.standaloneSetup(new CourseController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidFromDateIsValidationError() throws Exception {
        mvc.perform(get("/api/v1/courses").param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service, actors);
    }

    @Test
    void invalidToDateIsValidationError() throws Exception {
        mvc.perform(get("/api/v1/courses").param("to", "2026-99-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service, actors);
    }
}
