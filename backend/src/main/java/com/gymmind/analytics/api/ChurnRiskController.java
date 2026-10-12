package com.gymmind.analytics.api;
import com.gymmind.analytics.application.ChurnRiskService;
import com.gymmind.analytics.domain.*;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/analytics/churn-risks") @SecurityRequirement(name = "bearerAuth")
public class ChurnRiskController {
    private final ChurnRiskService service; private final CurrentActorProvider actors;
    public ChurnRiskController(ChurnRiskService service, CurrentActorProvider actors) { this.service=service; this.actors=actors; }
    @PostMapping public ApiResponse<ChurnRiskResult> assess(@Valid @RequestBody Request r) {
        return ApiResponse.success(service.assess(actors.requireCurrent(), new ChurnSignals(
                r.daysSinceLastCheckIn(),
                r.checkInsLast30Days(),
                r.checkInsPrevious30Days(),
                r.bookingsLast30Days(),
                r.courseParticipationsLast30Days(),
                r.membershipDaysRemaining(),
                r.membershipExpiring())));
    }

    public record Request(
            @Min(0) int daysSinceLastCheckIn,
            @Min(0) int checkInsLast30Days,
            @Min(0) int checkInsPrevious30Days,
            @Min(0) int bookingsLast30Days,
            @Min(0) int courseParticipationsLast30Days,
            @Min(0) int membershipDaysRemaining,
            boolean membershipExpiring) {
    }
}
