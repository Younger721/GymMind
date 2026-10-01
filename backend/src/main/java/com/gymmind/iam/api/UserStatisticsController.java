package com.gymmind.iam.api;

import com.gymmind.iam.application.UserStatisticsService;
import com.gymmind.iam.application.UserStatisticsView;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
public class UserStatisticsController {
    private final UserStatisticsService service;
    private final CurrentActorProvider actors;

    public UserStatisticsController(UserStatisticsService service, CurrentActorProvider actors) {
        this.service = Objects.requireNonNull(service, "service");
        this.actors = Objects.requireNonNull(actors, "actors");
    }

    @GetMapping("/statistics")
    public ApiResponse<UserStatisticsView> statistics() {
        return ApiResponse.success(service.statistics(actors.requireCurrent()));
    }
}
