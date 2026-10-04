package com.gymmind.course.api;

import com.gymmind.course.application.CourseService;
import com.gymmind.course.application.CourseView;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.iam.domain.model.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void platformAdminCreateIsRejected() throws Exception {
        CurrentActor actor = new CurrentActor(1L, null, java.util.Set.of(RoleCode.PLATFORM_ADMIN), java.util.Set.of(), 0L, "token");
        when(actors.requireCurrent()).thenReturn(actor);
        when(service.create(eq(actor), any())).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

        mvc.perform(post("/api/v1/courses").contentType(MediaType.APPLICATION_JSON).content("""
                {"tenantId":22,"coachId":8,"title":"Run","type":"GROUP","description":"Intervals",
                 "startsAt":"2026-10-01T10:00:00Z","endsAt":"2026-10-01T11:00:00Z","capacity":10,"location":"Studio"}
                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void coachCreatePassesCommandToService() throws Exception {
        CurrentActor actor = new CurrentActor(7L, 11L, java.util.Set.of(RoleCode.COACH), java.util.Set.of(), 0L, "token");
        when(actors.requireCurrent()).thenReturn(actor);
        when(service.create(eq(actor), any())).thenReturn(mock(CourseView.class));

        mvc.perform(post("/api/v1/courses").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Run","type":"GROUP","description":"Intervals",
                 "startsAt":"2026-10-01T10:00:00Z","endsAt":"2026-10-01T11:00:00Z","capacity":10,"location":"Studio"}
                """))
                .andExpect(status().isCreated());

        verify(service).create(eq(actor), argThat(command -> command.tenantId() == null
                && command.coachId() == null
                && command.description().equals("Intervals")));
    }
}
