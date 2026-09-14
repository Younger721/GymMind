package com.gymmind.analytics.api;
import com.gymmind.analytics.application.ChurnRiskService;
import com.gymmind.analytics.domain.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/analytics/churn-risks")
public class ChurnRiskController {
    private final ChurnRiskService service; private final CurrentActorProvider actors;
    public ChurnRiskController(ChurnRiskService service, CurrentActorProvider actors) { this.service=service; this.actors=actors; }
    @PostMapping public ApiResponse<ChurnRiskResult> assess(@Valid @RequestBody Request r) {
        return ApiResponse.success(service.assess(actors.requireCurrent(), new ChurnSignals(r.daysSinceLastWorkout(), r.workoutsLast30Days(), r.workoutsPrevious30Days(), r.membershipDaysRemaining(), r.membershipExpiring())));
    }
    public record Request(@Min(0) int daysSinceLastWorkout, @Min(0) int workoutsLast30Days, @Min(0) int workoutsPrevious30Days, @Min(0) int membershipDaysRemaining, boolean membershipExpiring) { }
}
