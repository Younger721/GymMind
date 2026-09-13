package com.gymmind.coach.api;
import com.gymmind.coach.application.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/coaches") @SecurityRequirement(name = "bearerAuth")
public class CoachController {
    private final CoachService coaches; private final CoachAssignmentService assignments; private final CurrentActorProvider actors;
    public CoachController(CoachService coaches, CoachAssignmentService assignments, CurrentActorProvider actors) { this.coaches = coaches; this.assignments = assignments; this.actors = actors; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<CoachView> create(@Valid @RequestBody CreateRequest request) { return ApiResponse.success(coaches.create(actors.requireCurrent(), new CreateCoachCommand(null, request.coachNumber(), request.fullName(), request.phone(), request.userId()))); }
    @GetMapping("/{coachId}") public ApiResponse<CoachView> find(@PathVariable Long coachId) { return ApiResponse.success(coaches.find(actors.requireCurrent(), coachId)); }
    @PatchMapping("/{coachId}") public ApiResponse<Void> update(@PathVariable Long coachId, @Valid @RequestBody UpdateRequest request) { coaches.update(actors.requireCurrent(), coachId, request.fullName(), request.phone()); return ApiResponse.success(null); }
    @PostMapping("/{coachId}/members/{memberId}") public ApiResponse<Void> assign(@PathVariable Long coachId, @PathVariable Long memberId) { assignments.assign(actors.requireCurrent(), new AssignCoachCommand(null, coachId, memberId)); return ApiResponse.success(null); }
    @DeleteMapping("/{coachId}/members/{memberId}") public ApiResponse<Void> unassign(@PathVariable Long coachId, @PathVariable Long memberId) { assignments.unassign(actors.requireCurrent(), coachId, memberId); return ApiResponse.success(null); }
    public record CreateRequest(@NotBlank String coachNumber, @NotBlank String fullName, @NotBlank String phone, Long userId) {}
    public record UpdateRequest(@NotBlank String fullName, @NotBlank String phone) {}
}
