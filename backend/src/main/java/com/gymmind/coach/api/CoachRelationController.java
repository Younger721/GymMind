package com.gymmind.coach.api;

import com.gymmind.coach.application.CoachAssignmentService;
import com.gymmind.coach.domain.model.CoachAssignment;
import com.gymmind.coach.domain.model.CoachAssignmentStatus;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coach-relations")
public class CoachRelationController {
    private final CoachAssignmentService service;
    private final CurrentActorProvider actors;

    public CoachRelationController(CoachAssignmentService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<View> request(@Valid @RequestBody Request body) {
        return ApiResponse.success(View.from(service.request(actors.requireCurrent(), body.coachId())));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id) { service.accept(actors.requireCurrent(), id); return ApiResponse.success(null); }
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id) { service.reject(actors.requireCurrent(), id); return ApiResponse.success(null); }
    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id) { service.cancel(actors.requireCurrent(), id); return ApiResponse.success(null); }
    @PostMapping("/{id}/end")
    public ApiResponse<Void> end(@PathVariable Long id) { service.end(actors.requireCurrent(), id); return ApiResponse.success(null); }

    @GetMapping("/pending")
    public ApiResponse<List<View>> pending() { return ApiResponse.success(view(service.list(actors.requireCurrent(), CoachAssignmentStatus.PENDING))); }
    @GetMapping("/active")
    public ApiResponse<List<View>> active() { return ApiResponse.success(view(service.list(actors.requireCurrent(), CoachAssignmentStatus.ACTIVE))); }

    private static List<View> view(List<CoachAssignment> values) { return values.stream().map(View::from).toList(); }
    public record Request(@NotNull Long coachId) {}
    public record View(Long id, Long tenantId, Long coachId, Long memberId, CoachAssignmentStatus status,
                       Instant requestedAt, Instant resolvedAt, Instant createdAt, Instant updatedAt) {
        static View from(CoachAssignment a) { return new View(a.getId(), a.getTenantId(), a.getCoachId(), a.getMemberId(), a.getStatus(), a.getRequestedAt(), a.getResolvedAt(), a.getCreatedAt(), a.getUpdatedAt()); }
    }
}
