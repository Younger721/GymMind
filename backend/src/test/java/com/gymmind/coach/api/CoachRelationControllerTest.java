package com.gymmind.coach.api;

import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.error.GlobalExceptionHandler;
import com.gymmind.shared.security.CurrentActorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.gymmind.support.SecurityTestActors.gymAdmin;
import static com.gymmind.support.SecurityTestActors.platformAdmin;
import static com.gymmind.support.SecurityTestActors.tenantActor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.Set;

class CoachRelationControllerTest {
    private CoachAssignmentService service;
    private CurrentActorProvider actors;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(CoachAssignmentService.class);
        actors = mock(CurrentActorProvider.class);
        mvc = MockMvcBuilders.standaloneSetup(new CoachRelationController(service, actors))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void memberCanRequestCoachAndResponseUsesApiContract() throws Exception {
        when(actors.requireCurrent()).thenReturn(tenantActor(10L, Set.of(RoleCode.MEMBER), Set.of("coach-relation:request")));
        mvc.perform(post("/api/v1/coach-relations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coachId\":7}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"));
        verify(service).request(any(), eq(7L));
    }

    @Test
    void missingCoachIdIsValidationError() throws Exception {
        mvc.perform(post("/api/v1/coach-relations").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(service, actors);
    }

    @Test
    void coachCanAcceptOrRejectPendingRelation() throws Exception {
        when(actors.requireCurrent()).thenReturn(tenantActor(10L, Set.of(RoleCode.COACH), Set.of("coach-relation:accept", "coach-relation:reject")));
        mvc.perform(post("/api/v1/coach-relations/3/accept")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/coach-relations/4/reject")).andExpect(status().isOk());
        verify(service).accept(any(), eq(3L));
        verify(service).reject(any(), eq(4L));
    }

    @Test
    void gymAndPlatformAdminsCanReadTenantRelations() throws Exception {
        when(service.list(any(), eq(CoachAssignmentStatus.PENDING))).thenReturn(List.of());
        when(actors.requireCurrent()).thenReturn(gymAdmin());
        mvc.perform(get("/api/v1/coach-relations/pending")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        when(actors.requireCurrent()).thenReturn(platformAdmin());
        when(service.list(any(), eq(CoachAssignmentStatus.ACTIVE))).thenReturn(List.of());
        mvc.perform(get("/api/v1/coach-relations/active")).andExpect(status().isOk());
        verify(service).list(gymAdmin(), CoachAssignmentStatus.PENDING);
        verify(service).list(platformAdmin(), CoachAssignmentStatus.ACTIVE);
    }

    @Test
    void forbiddenServiceErrorMapsToForbidden() throws Exception {
        when(actors.requireCurrent()).thenReturn(tenantActor(10L, Set.of(RoleCode.MEMBER), Set.of("coach-relation:read")));
        doThrow(new BusinessException(ErrorCode.FORBIDDEN)).when(service).accept(any(), eq(9L));
        mvc.perform(post("/api/v1/coach-relations/9/accept"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
