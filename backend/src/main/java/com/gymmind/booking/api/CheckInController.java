package com.gymmind.booking.api;

import com.gymmind.booking.application.CheckInService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/check-ins")
@SecurityRequirement(name = "bearerAuth")
public class CheckInController {
    private final CheckInService service;
    private final CurrentActorProvider actors;

    public CheckInController(CheckInService service, CurrentActorProvider actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> checkIn(@Valid @RequestBody CheckInRequest request) {
        service.checkIn(actors.requireCurrent(), request.memberId(), request.courseId());
        return ApiResponse.success(null);
    }

    public record CheckInRequest(@NotNull Long memberId, @NotNull Long courseId) {}
}
